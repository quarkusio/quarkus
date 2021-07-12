package io.quarkus.kotlin.serialization

import io.quarkus.runtime.annotations.ConfigRoot
import io.quarkus.runtime.annotations.ConfigItem
import io.quarkus.runtime.annotations.ConfigPhase.BUILD_AND_RUN_TIME_FIXED

@ConfigRoot(name = KotlinSerializationConfig.CONFIG_NAME, phase = BUILD_AND_RUN_TIME_FIXED)
class KotlinSerializationConfig {
    companion object {
        const val CONFIG_NAME = "kotlin-serialization"
    }

    /**
     * Configuration element for serializing to json
     */
    @ConfigItem(name = "json")
    @JvmField
    var json: JsonConfiguration = JsonConfiguration()

    override fun toString(): String {
        return "KotlinSerializationConfig(json=$json)"
    }
}