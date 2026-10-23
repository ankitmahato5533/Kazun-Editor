package com.kazun.editor

import com.kazun.editor.model.Clip
import com.kazun.editor.model.Project
import com.kazun.editor.model.Track
import com.kazun.editor.model.TrackType
import com.kazun.editor.timeline.TimelineOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineOpsTest {

    private fun clip(start: Long, len: Long = 1000, id: String = "c$start") = Clip(
        id = id,
        sourceUri = "content://test/$id",
        sourceDurationMs = 10_000,
        trimStartMs = 0,
        trimEndMs = len,
        timelineStartMs = start
    )

    private fun project(vararg clips: Clip): Project {
        val p = Project.new("t")
        val videoTrack = p.tracks.first { it.type == TrackType.VIDEO }
        var out = p
        clips.forEach { out = TimelineOps.addClip(out, videoTrack.id, it) }
        return out
    }

    private fun videoTrack(p: Project): Track = p.tracks.first { it.type == TrackType.VIDEO }

    @Test
    fun `add clip places at requested start when free`() {
        val p = project(clip(0))
        assertEquals(0L, videoTrack(p).clips.single().timelineStartMs)
    }

    @Test
    fun `add clip pushes right when overlapping`() {
        val p = project(clip(0, 1000, "a"), clip(500, 1000, "b"))
        val b = videoTrack(p).clips.first { it.id == "b" }
        assertEquals(1000L, b.timelineStartMs)
    }

    @Test
    fun `trim end clamps to source duration`() {
        val p = project(clip(0))
        val id = videoTrack(p).clips.single().id
        val out = TimelineOps.trimEnd(p, id, 99_999)
        assertEquals(10_000L, videoTrack(out).clips.single().trimEndMs)
    }

    @Test
    fun `trim start cannot pass trim end`() {
        val p = project(clip(0))
        val id = videoTrack(p).clips.single().id
        val out = TimelineOps.trimStart(p, id, 5_000)
        assertTrue(videoTrack(out).clips.single().trimStartMs < videoTrack(out).clips.single().trimEndMs)
    }

    @Test
    fun `split produces two clips that fill original range`() {
        val p = project(clip(0, 2000))
        val id = videoTrack(p).clips.single().id
        val out = TimelineOps.split(p, id, 1000)
        val clips = videoTrack(out).clips
        assertEquals(2, clips.size)
        assertEquals(0L, clips[0].timelineStartMs)
        assertEquals(1000L, clips[0].durationMs)
        assertEquals(1000L, clips[1].timelineStartMs)
        assertEquals(1000L, clips[1].durationMs)
        assertEquals(2000L, out.durationMs)
    }

    @Test
    fun `split outside bounds is a no-op`() {
        val p = project(clip(0, 2000))
        val id = videoTrack(p).clips.single().id
        assertEquals(p, TimelineOps.split(p, id, 5000))
    }

    @Test
    fun `move clamps between neighbours`() {
        val p = project(clip(0, 1000, "a"), clip(1000, 1000, "b"), clip(2000, 1000, "c"))
        val out = TimelineOps.moveClip(p, "b", 5000)
        assertEquals(1000L, videoTrack(out).clips.first { it.id == "b" }.timelineStartMs)
    }

    @Test
    fun `move cannot go negative`() {
        val p = project(clip(0, 1000, "a"), clip(2000, 1000, "b"))
        val out = TimelineOps.moveClip(p, "b", -500)
        assertEquals(1000L, videoTrack(out).clips.first { it.id == "b" }.timelineStartMs)
    }

    @Test
    fun `duration follows longest track`() {
        val p = project(clip(0, 1500), clip(2000, 3000))
        assertEquals(5000L, p.durationMs)
    }

    @Test
    fun `clipAt finds clip or null in gap`() {
        val p = project(clip(0, 1000))
        val t = videoTrack(p)
        assertNotNull(TimelineOps.clipAt(t, 500))
        assertNull(TimelineOps.clipAt(t, 1500))
    }

    @Test
    fun `effects are capped at six`() {
        var p = project(clip(0))
        val id = videoTrack(p).clips.single().id
        repeat(10) { i ->
            p = TimelineOps.addEffect(p, id, com.kazun.editor.model.EffectInstance(effectId = "e$i"))
        }
        assertEquals(6, videoTrack(p).clips.single().effects.size)
    }

    @Test
    fun `random operation sequence keeps invariants`() {
        var p = project(clip(0, 800, "a"))
        val rnd = java.util.Random(42)
        repeat(120) {
            val t = videoTrack(p)
            when (rnd.nextInt(4)) {
                0 -> p = TimelineOps.addClip(
                    p, t.id, clip(rnd.nextLong().mod(5000).toLong(), 300 + rnd.nextInt(700))
                )
                1 -> t.clips.randomOrNull(rnd)?.let { p = TimelineOps.moveClip(p, it.id, rnd.nextLong().mod(6000).toLong()) }
                2 -> t.clips.randomOrNull(rnd)?.let { p = TimelineOps.split(p, it.id, it.timelineStartMs + it.durationMs / 2) }
                3 -> t.clips.randomOrNull(rnd)?.let { p = TimelineOps.removeClip(p, it.id) }
            }
            val clips = videoTrack(p).clips.sortedBy { c -> c.timelineStartMs }
            clips.forEach { c ->
                assertTrue("start >= 0", c.timelineStartMs >= 0)
                assertTrue("trim valid", c.trimStartMs < c.trimEndMs)
                assertTrue("trim within source", c.trimEndMs <= c.sourceDurationMs)
            }
            clips.zipWithNext().forEach { (a, b) ->
                assertTrue("no overlap ${a.id}/${b.id}", a.timelineEndMs <= b.timelineStartMs)
            }
        }
    }

    private fun List<Clip>.randomOrNull(rnd: java.util.Random): Clip? =
        if (isEmpty()) null else this[rnd.nextInt(size)]
}
