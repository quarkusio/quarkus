package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime

import io.quarkus.arc.All
import io.quarkus.arc.DefaultBean
import io.quarkus.resteasy.reactive.kotlin.serialization.common.JsonBuilderCustomizer
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@Singleton
class JsonProducer {

    @ExperimentalSerializationApi
    @Singleton
    @Produces
    @DefaultBean
    fun defaultJson(configuration: KotlinSerializationConfig, @All customizers: java.util.List<JsonBuilderCustomizer>) = Json {
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

        val sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers)
        for (customizer in sortedCustomizers) {
            customizer.customize(this)
        }
    }

    private fun sortCustomizersInDescendingPriorityOrder(
        customizers: Iterable<JsonBuilderCustomizer>
    ): List<JsonBuilderCustomizer> {
        val sortedCustomizers: MutableList<JsonBuilderCustomizer> = ArrayList()
        for (customizer in customizers) {
            sortedCustomizers.add(customizer)
        }
        sortedCustomizers.sort()
        return sortedCustomizers
    }
}
