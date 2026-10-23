package com.kazun.editor.export

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.kazun.editor.effects.EffectCatalog
import com.kazun.editor.effects.EffectMapper
import com.kazun.editor.model.Project
import com.kazun.editor.model.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real export with effects baked in. Uses Media3 Transformer, which drives the
 * hardware encoder and runs the same GPU shader chain as the preview.
 */
@OptIn(UnstableApi::class)
class TransformerExporter(private val context: Context) {

    suspend fun export(
        project: Project,
        catalog: EffectCatalog,
        output: File,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.Main) {
        val videoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO }
            ?: throw IllegalStateException("No video track")
        val clips = videoTrack.clips.sortedBy { it.timelineStartMs }
        require(clips.isNotEmpty()) { "Timeline is empty" }

        val items = clips.map { clip ->
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimEndMs)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(clip.sourceUri)
                .setClippingConfiguration(clipping)
                .build()
            EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        emptyList(),
                        EffectMapper.mapClip(context, clip, catalog)
                    )
                )
                .build()
        }

        val sequence = EditedMediaItemSequence(items)
        val presentation = Presentation.createForWidthAndHeight(
            project.width,
            project.height,
            Presentation.LAYOUT_SCALE_TO_FIT
        )
        val composition = Composition.Builder(listOf(sequence))
            .setEffects(Effects(emptyList(), listOf(presentation)))
            .build()

        if (output.exists()) output.delete()

        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        if (continuation.isActive) continuation.resume(output)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(exportException.message, exportException)
                            )
                        }
                    }
                })
                .build()

            continuation.invokeOnCancellation { transformer.cancel() }

            val progressHolder = androidx.media3.transformer.ProgressHolder()
            val ticker = Thread {
                while (continuation.isActive) {
                    if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(progressHolder.progress / 100f)
                    }
                    Thread.sleep(250)
                }
            }.apply { isDaemon = true; start() }

            transformer.start(composition, output.absolutePath)
        }
    }
}
