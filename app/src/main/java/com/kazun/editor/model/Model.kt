package com.kazun.editor.model

import kotlinx.serialization.Serializable
import java.util.UUID

enum class TrackType { VIDEO, OVERLAY, TEXT, AUDIO }
enum class ClipKind { VIDEO, IMAGE, TEXT }

@Serializable
data class TextProps(
    val content: String = "Your text",
    val fontSize: Float = 48f,
    val color: Long = 0xFFFFFFFF,
    val bgColor: Long = 0x66000000,
    val bold: Boolean = true,
    val alignment: String = "CENTER"
)

@Serializable
data class EffectInstance(
    val id: String = UUID.randomUUID().toString(),
    val effectId: String,
    val enabled: Boolean = true,
    val intensity: Float = 1f,
    val params: Map<String, Float> = emptyMap(),
    val options: Map<String, String> = emptyMap()
)

@Serializable
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val displayName: String = "",
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0,
    val trimEndMs: Long,
    val timelineStartMs: Long = 0,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val kind: ClipKind = ClipKind.VIDEO,
    val effects: List<EffectInstance> = emptyList(),
    val text: TextProps? = null
) {
    val durationMs: Long get() = (((trimEndMs - trimStartMs).coerceAtLeast(0)) / speed).toLong()
    val timelineEndMs: Long get() = timelineStartMs + durationMs
}

@Serializable
data class Track(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackType,
    val muted: Boolean = false,
    val clips: List<Clip> = emptyList()
) {
    val durationMs: Long get() = clips.maxOfOrNull { it.timelineEndMs } ?: 0L
}

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled project",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val tracks: List<Track> = emptyList(),
    val version: Int = 1
) {
    val durationMs: Long get() = tracks.maxOfOrNull { it.durationMs } ?: 0L

    companion object {
        fun new(name: String, aspect: String = "16:9"): Project {
            val (w, h) = when (aspect) {
                "9:16" -> 1080 to 1920
                "1:1" -> 1080 to 1080
                "4:5" -> 1080 to 1350
                else -> 1920 to 1080
            }
            return Project(
                name = name,
                width = w,
                height = h,
                tracks = listOf(
                    Track(type = TrackType.VIDEO),
                    Track(type = TrackType.TEXT),
                    Track(type = TrackType.AUDIO)
                )
            )
        }
    }
}
