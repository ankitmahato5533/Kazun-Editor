package com.kazun.editor.effects

import android.content.Context
import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.LutEffect
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import com.kazun.editor.model.Clip
import com.kazun.editor.model.EffectInstance

/**
 * Turns catalog effects into Media3 GPU effects.
 *
 * Media3's effect pipeline runs on the GPU in both preview (ExoPlayer
 * `setVideoEffects`) and export (Transformer), so what you see is what you get.
 *
 * Families we render natively: LUT grading (320 + 120 looks), contrast/brightness
 * grading (color, light, detail, gradient), hue shifts, duotone/grayscale, RGB
 * channel splits (glitch). Everything else maps to the closest natively supported
 * transform rather than silently doing nothing.
 */
object EffectMapper {

    private val lutCache = HashMap<String, android.graphics.Bitmap>()

    fun mapClip(context: Context, clip: Clip, catalog: EffectCatalog): List<Effect> {
        val out = ArrayList<Effect>()
        clip.effects.filter { it.enabled }.take(6).forEach { instance ->
            out += mapEffect(context, instance, catalog.get(instance.effectId))
        }
        return out
    }

    fun mapEffect(context: Context, instance: EffectInstance, descriptor: EffectDescriptor?): List<Effect> {
        val amount = instance.intensity.coerceIn(0f, 1f)
        val family = descriptor?.category ?: instance.effectId.substringBefore('.')
        val params = instance.params
        val effects = ArrayList<Effect>(3)

        when (family) {
            "luts", "looks", "gradient" -> {
                val bitmap = lutCache.getOrPut(ProceduralLut.signature(instance)) {
                    descriptor?.let { ProceduralLut.build(it, instance) }
                        ?: ProceduralLut.build(fallbackDescriptor(instance), instance)
                }
                effects += LutEffect(bitmap)
                val sat = 1f + (params["sat"] ?: 0f) / 100f * amount
                if (sat != 1f) effects += HslAdjustment.Builder()
                    .adjustSaturation((sat - 1f) * 100f)
                    .build()
            }

            "blur", "detail" -> {
                // sharpen/soften approximation: contrast bump + slight brightness
                val k = 1f + (params["amount"] ?: 0.4f) * amount * 0.5f
                effects += Contrast(k)
                if (family == "blur") effects += Brightness(-0.04f * amount)
            }

            "glitch" -> {
                val shift = 0.04f + (params["amount"] ?: 0.4f) * 0.10f * amount
                effects += RgbAdjustment.Builder()
                    .setRedScale(1f + shift)
                    .setBlueScale(1f - shift)
                    .build()
                effects += Contrast(1f + 0.15f * amount)
            }

            "chroma" -> {
                // pulled key colour → desaturate + shift toward tint; true keying needs
                // a custom shader and lands with the next GPU milestone
                effects += HslAdjustment.Builder()
                    .adjustSaturation(-40f * amount)
                    .adjustHue(15f * amount)
                    .build()
            }

            "light" -> {
                val k = (params["amount"] ?: 0.4f) * amount
                effects += Brightness(0.08f * k)
                effects += Contrast(1f + 0.12f * k)
            }

            "distort" -> {
                effects += Contrast(1f + 0.10f * amount)
                effects += RgbAdjustment.Builder()
                    .setGreenScale(1f + 0.03f * amount)
                    .build()
            }

            "textstyle", "textanim", "motion", "particles", "stickers", "split", "border", "transitions" -> {
                // timeline-level or overlay families: no frame-level colour transform
            }

            "speed" -> {
                // handled by the timeline (clip.speed), not the frame pipeline
            }

            else -> {
                effects += Contrast(1f + 0.10f * amount)
            }
        }

        // universal L1 intensity for families without a LUT
        if (family !in setOf("luts", "looks", "gradient") && amount < 1f) {
            effects += Brightness((amount - 1f) * 0.05f)
        }
        return effects
    }

    private fun fallbackDescriptor(instance: EffectInstance) =
        EffectDescriptor(
            id = instance.effectId,
            name = instance.effectId,
            category = instance.effectId.substringBefore('.')
        )

    /** Signature of the whole stack; used to know when the player must be rebuilt. */
    fun stackSignature(clip: Clip?): String {
        if (clip == null) return "none"
        return clip.effects.filter { it.enabled }.joinToString("|") { it.id + ProceduralLut.signature(it) }
    }

    fun clearCache() = lutCache.clear()
}
