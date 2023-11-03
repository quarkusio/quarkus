package io.quarkus.it.resteasy.reactive.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.jackson.ObjectMapperCustomizer
import io.quarkus.runtime.annotations.StaticInitSafe
import jakarta.inject.Singleton
import org.eclipse.microprofile.config.inject.ConfigProperty

@Singleton
class RegisterCustomModuleCustomizer : ObjectMapperCustomizer {
    @StaticInitSafe @ConfigProperty(name = "test.prop") lateinit var testProp: String

    override fun customize(objectMapper: ObjectMapper) {
        GreetingResource.MY_PROPERTY.set(testProp)
    }
}
