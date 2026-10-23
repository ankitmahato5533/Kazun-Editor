package com.kazun.editor.effects

import com.kazun.editor.model.EffectInstance

object EffectDefaults {
    fun instanceOf(descriptor: EffectDescriptor): EffectInstance {
        val numbers = descriptor.params
            .filter { it.type in setOf("FLOAT", "INT", "BOOL", "ANGLE", "SEED", "POINT2D") }
            .associate { it.key to it.default }
        val options = descriptor.params
            .filter { it.type == "ENUM" }
            .associate { it.key to (it.defaultOption.ifEmpty { it.options.firstOrNull().orEmpty() }) }
        return EffectInstance(
            effectId = descriptor.id,
            intensity = 1f,
            params = numbers,
            options = options
        )
    }
}
