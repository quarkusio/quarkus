package io.quarkus.kotlin.serialization

import io.quarkus.runtime.annotations.Recorder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.util.function.Supplier

@ExperimentalSerializationApi
@Recorder
open class SerializerRecorder {
    fun configFactory(configuration: KotlinSerializationConfig) = JsonSupplier(configuration)
}

class JsonSupplier(var configuration: KotlinSerializationConfig) : Supplier<Json> {
    @Suppress("unused")
    constructor() : this(
        KotlinSerializationConfig()
    )

    override fun get(): Json {
        return Json {
            encodeDefaults = configuration.json.encodeDefaults
            ignoreUnknownKeys = configuration.json.ignoreUnknownKeys
            isLenient = configuration.json.isLenient
            allowStructuredMapKeys = configuration.json.allowStructuredMapKeys
            prettyPrint = configuration.json.prettyPrint
            prettyPrintIndent = configuration.json.prettyPrintIndent
            coerceInputValues = configuration.json.coerceInputValues
            useArrayPolymorphism = configuration.json.useArrayPolymorphism
            classDiscriminator = configuration.json.classDiscriminator
            allowSpecialFloatingPointValues = configuration.json.allowSpecialFloatingPointValues
            useAlternativeNames = configuration.json.useAlternativeNames
        }
    }
}
