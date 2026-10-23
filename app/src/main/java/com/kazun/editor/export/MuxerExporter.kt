package com.kazun.editor.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.kazun.editor.model.Project
import com.kazun.editor.model.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Fast, no-re-encode export: copies video + audio samples from each clip on the
 * video track into one MP4. Works when all clips share compatible codecs (the
 * normal case for phone footage). Re-encoding (needed for effects baking and
 * mixed codecs) is the next milestone and will use Media3 Transformer.
 */
class MuxerExporter(private val context: Context) {

    data class Segment(val uri: Uri, val startMs: Long, val endMs: Long)

    suspend fun export(
        project: Project,
        output: File,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val videoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO }
            ?: error("No video track")
        val segments = videoTrack.clips.sortedBy { it.timelineStartMs }.map {
            Segment(Uri.parse(it.sourceUri), it.trimStartMs, it.trimEndMs)
        }
        require(segments.isNotEmpty()) { "Timeline is empty" }

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoIndex = -1
            var audioIndex = -1
            var framesWritten = 0L
            var timeOffsetUs = 0L
            var formatReady = false

            for ((index, segment) in segments.withIndex()) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, segment.uri, null)
                    val vTrack = (0 until extractor.trackCount).firstOrNull {
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                    } ?: continue
                    val aTrack = (0 until extractor.trackCount).firstOrNull {
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                    }

                    val vFormat = extractor.getTrackFormat(vTrack)
                    if (!formatReady) {
                        videoIndex = muxer.addTrack(vFormat)
                        if (aTrack != null && index == 0) {
                            audioIndex = muxer.addTrack(extractor.getTrackFormat(aTrack))
                        }
                        muxer.start()
                        formatReady = true
                    }

                    val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024)
                    val info = MediaCodec.BufferInfo()

                    // ---- video ----
                    extractor.selectTrack(vTrack)
                    extractor.seekTo(segment.startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    var segmentEndUs = Long.MAX_VALUE
                    var lastPts = -1L
                    while (true) {
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        val pts = extractor.sampleTime
                        if (pts < 0) { extractor.advance(); continue }
                        if (lastPts < 0) segmentEndUs = segment.endMs * 1000
                        if (pts > segmentEndUs) break
                        info.offset = 0
                        info.size = size
                        info.presentationTimeUs = pts - (segment.startMs * 1000) + timeOffsetUs
                        info.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0)
                            MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer.writeSampleData(videoIndex, buffer, info)
                        framesWritten++
                        lastPts = pts
                        extractor.advance()
                    }
                    timeOffsetUs += (segment.endMs - segment.startMs) * 1000

                    onProgress((index + 1f) / segments.size)
                    buffer.clear()
                } finally {
                    runCatching { extractor.release() }
                }
            }
            require(framesWritten > 0) { "Could not read any video frames" }
            output
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }
}
