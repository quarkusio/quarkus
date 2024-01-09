package io.quarkus.resteasy.reactive.kotlin.serialization.runtime

import io.quarkus.resteasy.reactive.kotlin.serialization.common.JsonBuilderCustomizer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus

class ValidationJsonBuilderCustomizer : JsonBuilderCustomizer {
    @ExperimentalSerializationApi
    override fun customize(jsonBuilder: JsonBuilder) {
        jsonBuilder.serializersModule =
            jsonBuilder.serializersModule.plus(
                SerializersModule {
                    contextual(ViolationReportSerializer)
                    contextual(ViolationReportViolationSerializer)
                }
            )
    }
}
