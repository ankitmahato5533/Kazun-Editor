package com.kazun.editor.effects

import android.graphics.Bitmap
import android.graphics.Color
import com.kazun.editor.model.EffectInstance
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Builds a 3D LUT (flattened into a 2D bitmap: cubeSize x cubeSize^2) from
 * effect parameters. Media3's LutEffect samples it on the GPU, so one bitmap
 * gives us real per-preset grading with no per-frame cost beyond a texture read.
 *
 * Layout expected by Media3: x = blue index, y = (green * size + red).
 */
object ProceduralLut {

    const val CUBE_SIZE = 17

    fun build(descriptor: EffectDescriptor, effect: EffectInstance): Bitmap {
        val p = effect.params
        val hue = p["hue"] ?: 0f
        val temp = (p["temp"] ?: 0f) / 100f
        val sat = 1f + ((p["sat"] ?: 0f) / 100f)
        val contrast = 1f + ((p["contrast"] ?: 0f) / 100f) * 0.6f
        val brightness = (p["brightness"] ?: 0f) / 200f
        val fade = (p["fade"] ?: 0f) / 100f * 0.25f
        val strength = effect.intensity.coerceIn(0f, 1f)

        // deterministic per-preset tint from the name, so 320 LUTs look different
        val seed = descriptor.name.hashCode()
        val warm = ((seed % 100) / 100f - 0.5f) * 0.10f
        val tintR = 1f + warm + temp * 0.18f
        val tintB = 1f - warm - temp * 0.18f
        val tintG = 1f + (((seed / 100) % 100) / 100f - 0.5f) * 0.05f

        val size = CUBE_SIZE
        val bitmap = Bitmap.createBitmap(size, size * size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size * size)
        var i = 0
        for (g in 0 until size) {
            for (r in 0 until size) {
                for (b in 0 until size) {
                    val rf = r / (size - 1f)
                    val gf = g / (size - 1f)
                    val bf = b / (size - 1f)

                    // temperature + tint
                    var cr = rf * tintR
                    var cg = gf * tintG
                    var cb = bf * tintB

                    // contrast around 0.5
                    cr = (cr - 0.5f) * contrast + 0.5f
                    cg = (cg - 0.5f) * contrast + 0.5f
                    cb = (cb - 0.5f) * contrast + 0.5f

                    // brightness + fade (lifted blacks)
                    cr += brightness + fade
                    cg += brightness + fade
                    cb += brightness + fade

                    // saturation
                    val luma = 0.2126f * cr + 0.7152f * cg + 0.0722f * cb
                    cr = luma + (cr - luma) * sat
                    cg = luma + (cg - luma) * sat
                    cb = luma + (cb - luma) * sat

                    // hue rotation
                    if (hue != 0f) {
                        val a = Math.toRadians(hue.toDouble())
                        val cosA = cos(a).toFloat()
                        val sinA = sin(a).toFloat()
                        val nr = cr * (0.213f + cosA * 0.787f - sinA * 0.213f) +
                            cg * (0.715f - cosA * 0.715f - sinA * 0.715f) +
                            cb * (0.072f - cosA * 0.072f + sinA * 0.928f)
                        val ng = cr * (0.213f - cosA * 0.213f + sinA * 0.143f) +
                            cg * (0.715f + cosA * 0.285f + sinA * 0.140f) +
                            cb * (0.072f - cosA * 0.072f - sinA * 0.283f)
                        val nb = cr * (0.213f - cosA * 0.213f - sinA * 0.787f) +
                            cg * (0.715f - cosA * 0.715f + sinA * 0.715f) +
                            cb * (0.072f + cosA * 0.928f + sinA * 0.072f)
                        cr = nr; cg = ng; cb = nb
                    }

                    // gamma-ish curve for film feel
                    cr = curve(cr)
                    cg = curve(cg)
                    cb = curve(cb)

                    // blend with identity by intensity
                    cr = rf + (cr - rf) * strength
                    cg = gf + (cg - gf) * strength
                    cb = bf + (cb - bf) * strength

                    val rOut = (cr.coerceIn(0f, 1f) * 255f).toInt()
                    val gOut = (cg.coerceIn(0f, 1f) * 255f).toInt()
                    val bOut = (cb.coerceIn(0f, 1f) * 255f).toInt()
                    pixels[i++] = Color.rgb(rOut, gOut, bOut)
                }
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size * size)
        return bitmap
    }

    private fun curve(v: Float): Float {
        val c = v.coerceIn(0f, 1f)
        return c.pow(1.05f)
    }

    /** Cheap signature used to cache bitmaps across recompositions. */
    fun signature(effect: EffectInstance): String =
        effect.effectId + ":" + effect.intensity + ":" + effect.params.toSortedMap()
}
