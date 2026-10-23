package com.kazun.editor.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

data class ClipMeta(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val displayName: String
)

object MediaProbe {

    fun probe(context: Context, uri: Uri): ClipMeta {
        var name = "clip"
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) name = c.getString(0) ?: name }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            ClipMeta(durationMs = duration, width = w, height = h, displayName = name)
        } catch (t: Throwable) {
            ClipMeta(durationMs = 0L, width = 0, height = 0, displayName = name)
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun thumbnail(context: Context, uri: Uri, atMs: Long = 0L): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(atMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val tenth = (ms % 1000) / 100
        return "%02d:%02d.%d".format(m, s, tenth)
    }
}
