package io.quarkus.resteasy.reactive.kotlin.serialization.common.runtime

import io.quarkus.arc.All
import io.quarkus.arc.DefaultBean
import io.quarkus.resteasy.reactive.kotlin.serialization.common.JsonBuilderCustomizer
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import java.lang.Thread
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonNamingStrategy.Builtins

@Singleton
class JsonProducer {
    @ExperimentalSerializationApi
    @Singleton
    @Produces
    @DefaultBean
    fun defaultJson(
        configuration: KotlinSerializationConfig,
        @All customizers: java.util.List<JsonBuilderCustomizer>
    ) = Json {
        allowSpecialFloatingPointValues = configuration.json().allowSpecialFloatingPointValues()
        allowStructuredMapKeys = configuration.json().allowStructuredMapKeys()
        classDiscriminator = configuration.json().classDiscriminator()
        coerceInputValues = configuration.json().coerceInputValues()
        encodeDefaults = configuration.json().encodeDefaults()
        explicitNulls = configuration.json().explicitNulls()
        ignoreUnknownKeys = configuration.json().ignoreUnknownKeys()
        isLenient = configuration.json().isLenient()
        prettyPrint = configuration.json().prettyPrint()
        prettyPrintIndent = configuration.json().prettyPrintIndent()
        useAlternativeNames = configuration.json().useAlternativeNames()
        useArrayPolymorphism = configuration.json().useArrayPolymorphism()
        decodeEnumsCaseInsensitive = configuration.json().decodeEnumsCaseInsensitive()
        allowTrailingComma = configuration.json().allowTrailingComma()

        configuration.json().namingStrategy().ifPresent { strategy ->
            loadStrategy(this, strategy, this@JsonProducer)
        }
        val sortedCustomizers = sortCustomizersInDescendingPriorityOrder(customizers)
        for (customizer in sortedCustomizers) {
            customizer.customize(this)
        }
    }

    @ExperimentalSerializationApi
    private fun loadStrategy(
        jsonBuilder: JsonBuilder,
        strategy: String,
        jsonProducer: JsonProducer
    ) {
        val strategyProperty: KMutableProperty1<JsonBuilder, JsonNamingStrategy> =
            (JsonBuilder::class.memberProperties.find { member -> member.name == "namingStrategy" }
                ?: throw ReflectiveOperationException(
                    "Could not find the namingStrategy property on JsonBuilder"
                ))
                as KMutableProperty1<JsonBuilder, JsonNamingStrategy>
        strategyProperty.isAccessible = true

        strategyProperty.set(
            jsonBuilder,
            if (strategy.startsWith("JsonNamingStrategy")) {
                jsonProducer.extractBuiltIn(strategy)
            } else {
                jsonProducer.loadStrategyClass(strategy)
            }
        )
    }

    @ExperimentalSerializationApi
    private fun loadStrategyClass(strategy: String): JsonNamingStrategy {
        try {
            val strategyClass: Class<JsonNamingStrategy> =
                Thread.currentThread().contextClassLoader.loadClass(strategy)
                    as Class<JsonNamingStrategy>
            val constructor =
                strategyClass.constructors.find { it.parameterCount == 0 }
                    ?: throw ReflectiveOperationException(
                        "No no-arg constructor found on $strategy"
                    )
            return constructor.newInstance() as JsonNamingStrategy
        } catch (e: ReflectiveOperationException) {
            throw IllegalArgumentException(
                "Error loading naming strategy:  ${strategy.substringAfter('.')}",
                e
            )
        }
    }

    @ExperimentalSerializationApi
    private fun extractBuiltIn(strategy: String): JsonNamingStrategy {
        val kClass = Builtins::class
        val property =
            kClass.memberProperties.find { property ->
                property.name == strategy.substringAfter('.')
            }
                ?: throw IllegalArgumentException(
                    "Unknown naming strategy provided:  ${strategy.substringAfter('.')}"
                )

        return property.get(JsonNamingStrategy) as JsonNamingStrategy
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
