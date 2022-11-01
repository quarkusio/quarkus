package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime

import io.quarkus.arc.DefaultBean
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import javax.enterprise.inject.Produces
import javax.inject.Singleton

@Singleton
class JsonProducer {

    @ExperimentalSerializationApi
    @Singleton
    @Produces
    @DefaultBean
    fun json(configuration: KotlinSerializationConfig) = Json {
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
