package com.kazun.editor.timeline

import com.kazun.editor.model.Clip
import com.kazun.editor.model.EffectInstance
import com.kazun.editor.model.Project
import com.kazun.editor.model.Track

/**
 * Pure timeline operations. No Android dependencies — fully unit tested.
 * Every function returns a new immutable Project. Invariants:
 *  1. Clips on a track never overlap.
 *  2. 0 <= trimStart < trimEnd <= sourceDuration (+ 1s slack for images).
 *  3. timelineStart >= 0.
 */
object TimelineOps {

    private fun Project.mapTrack(trackId: String, f: (Track) -> Track): Project =
        copy(tracks = tracks.map { if (it.id == trackId) f(it) else it }, updatedAt = System.currentTimeMillis())

    private fun Project.mapClip(clipId: String, f: (Clip) -> Clip): Project = copy(
        tracks = tracks.map { t -> t.copy(clips = t.clips.map { if (it.id == clipId) f(it) else it }) },
        updatedAt = System.currentTimeMillis()
    )

    fun findClip(project: Project, clipId: String): Clip? =
        project.tracks.firstNotNullOfOrNull { t -> t.clips.firstOrNull { it.id == clipId } }

    fun findTrackOf(project: Project, clipId: String): Track? =
        project.tracks.firstOrNull { t -> t.clips.any { it.id == clipId } }

    fun addClip(project: Project, trackId: String, clip: Clip): Project {
        val placed = placeWithoutOverlap(project, trackId, clip)
        return project.mapTrack(trackId) { t ->
            t.copy(clips = (t.clips + placed).sortedBy { it.timelineStartMs })
        }
    }

    /** Pushes the incoming clip right until it does not overlap existing clips. */
    private fun placeWithoutOverlap(project: Project, trackId: String, clip: Clip): Clip {
        val track = project.tracks.firstOrNull { it.id == trackId } ?: return clip
        var start = clip.timelineStartMs.coerceAtLeast(0)
        val len = clip.durationMs
        var moved = true
        var guard = 0
        while (moved && guard++ < 500) {
            moved = false
            for (c in track.clips) {
                val aStart = start
                val aEnd = start + len
                if (aStart < c.timelineEndMs && aEnd > c.timelineStartMs) {
                    start = c.timelineEndMs
                    moved = true
                }
            }
        }
        return clip.copy(timelineStartMs = start)
    }

    fun removeClip(project: Project, clipId: String): Project = project.copy(
        tracks = project.tracks.map { t -> t.copy(clips = t.clips.filterNot { it.id == clipId }) },
        updatedAt = System.currentTimeMillis()
    )

    fun moveClip(project: Project, clipId: String, newStartMs: Long): Project {
        val clip = findClip(project, clipId) ?: return project
        val track = findTrackOf(project, clipId) ?: return project
        val start = freeStartFor(
            others = track.clips.filterNot { it.id == clipId },
            lengthMs = clip.durationMs,
            desiredStartMs = newStartMs
        )
        return project.mapClip(clipId) { it.copy(timelineStartMs = start) }
    }

    /**
     * Every free gap on a track is a (start, maxStart) pair. If the desired start
     * lands in one, use it; otherwise snap into the closest gap. This guarantees
     * clips never overlap whatever the drag does.
     */
    private fun freeStartFor(others: List<Clip>, lengthMs: Long, desiredStartMs: Long): Long {
        val sorted = others.sortedBy { it.timelineStartMs }
        val gaps = mutableListOf<Pair<Long, Long>>()
        var cursor = 0L
        for (o in sorted) {
            if (o.timelineStartMs - cursor >= lengthMs) {
                gaps.add(cursor to (o.timelineStartMs - lengthMs))
            }
            cursor = maxOf(cursor, o.timelineEndMs)
        }
        gaps.add(cursor to Long.MAX_VALUE / 4)

        val desired = desiredStartMs.coerceAtLeast(0)
        val exact = gaps.firstOrNull { desired in it.first..it.second }
        if (exact != null) return desired
        val nearest = gaps.minByOrNull { (a, b) ->
            minOf(kotlin.math.abs(a - desired), kotlin.math.abs(b - desired))
        } ?: return desired
        return desired.coerceIn(nearest.first, nearest.second)
    }

    fun trimStart(project: Project, clipId: String, newTrimStartMs: Long): Project {
        val clip = findClip(project, clipId) ?: return project
        val clamped = newTrimStartMs.coerceIn(0, (clip.trimEndMs - 100).coerceAtLeast(0))
        val deltaSource = clamped - clip.trimStartMs
        val deltaTimeline = (deltaSource / clip.speed).toLong()
        return project.mapClip(clipId) {
            it.copy(
                trimStartMs = clamped,
                timelineStartMs = (it.timelineStartMs + deltaTimeline).coerceAtLeast(0)
            )
        }
    }

    fun trimEnd(project: Project, clipId: String, newTrimEndMs: Long): Project {
        val clip = findClip(project, clipId) ?: return project
        val clamped = newTrimEndMs.coerceIn(clip.trimStartMs + 100, clip.sourceDurationMs)
        return project.mapClip(clipId) { it.copy(trimEndMs = clamped) }
    }

    /** Splits at an absolute timeline position. Returns project unchanged if outside clip. */
    fun split(project: Project, clipId: String, atTimelineMs: Long): Project {
        val clip = findClip(project, clipId) ?: return project
        val track = findTrackOf(project, clipId) ?: return project
        if (atTimelineMs <= clip.timelineStartMs + 50 || atTimelineMs >= clip.timelineEndMs - 50) return project

        val localSource = clip.trimStartMs + ((atTimelineMs - clip.timelineStartMs) * clip.speed).toLong()
        val left = clip.copy(trimEndMs = localSource)
        val right = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            trimStartMs = localSource,
            timelineStartMs = atTimelineMs
        )
        val updated = track.clips.filterNot { it.id == clipId } + left + right
        return project.mapTrack(track.id) { it.copy(clips = updated.sortedBy { c -> c.timelineStartMs }) }
    }

    fun addEffect(project: Project, clipId: String, effect: EffectInstance): Project =
        project.mapClip(clipId) { c -> c.copy(effects = (c.effects + effect).take(6)) }

    fun removeEffect(project: Project, clipId: String, effectInstanceId: String): Project =
        project.mapClip(clipId) { c -> c.copy(effects = c.effects.filterNot { it.id == effectInstanceId }) }

    fun updateEffect(project: Project, clipId: String, effect: EffectInstance): Project =
        project.mapClip(clipId) { c -> c.copy(effects = c.effects.map { if (it.id == effect.id) effect else it }) }

    fun setVolume(project: Project, clipId: String, volume: Float): Project =
        project.mapClip(clipId) { it.copy(volume = volume.coerceIn(0f, 2f)) }

    fun setText(project: Project, clipId: String, text: com.kazun.editor.model.TextProps): Project =
        project.mapClip(clipId) { it.copy(text = text) }

    /** Clip currently visible at a given playhead on a track (null if gap). */
    fun clipAt(track: Track, timeMs: Long): Clip? =
        track.clips.firstOrNull { timeMs >= it.timelineStartMs && timeMs < it.timelineEndMs }
}
