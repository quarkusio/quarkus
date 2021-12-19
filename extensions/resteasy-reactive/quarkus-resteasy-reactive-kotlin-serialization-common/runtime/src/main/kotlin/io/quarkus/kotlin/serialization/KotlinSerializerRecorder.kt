package io.quarkus.kotlin.serialization

import io.quarkus.runtime.annotations.Recorder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import java.util.function.Supplier

@Recorder
@ExperimentalSerializationApi
open class KotlinSerializerRecorder {
    open fun configFactory(configuration: KotlinSerializationConfig) = JsonSupplier(configuration)
}

@ExperimentalSerializationApi
open class JsonSupplier(open var configuration: KotlinSerializationConfig) : Supplier<Json> {
    @Suppress("unused")
    constructor() : this(KotlinSerializationConfig())

    override fun get(): Json {
        return Json {
            allowSpecialFloatingPointValues = configuration.json.allowSpecialFloatingPointValues
            allowStructuredMapKeys = configuration.json.allowStructuredMapKeys
            classDiscriminator = configuration.json.classDiscriminator
            coerceInputValues = configuration.json.coerceInputValues
            encodeDefaults = configuration.json.encodeDefaults
            explicitNulls = configuration.json.explicitNulls
            ignoreUnknownKeys = configuration.json.ignoreUnknownKeys
            isLenient = configuration.json.isLenient
            prettyPrint = configuration.json.prettyPrint
            prettyPrintIndent = configuration.json.prettyPrintIndent
            useAlternativeNames = configuration.json.useAlternativeNames
            useArrayPolymorphism = configuration.json.useArrayPolymorphism
        }
    }
}
