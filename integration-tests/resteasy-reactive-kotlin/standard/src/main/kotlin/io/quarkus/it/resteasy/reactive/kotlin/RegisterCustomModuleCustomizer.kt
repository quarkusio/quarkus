package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.jackson.JsonMapperBuilderCustomizer
import io.quarkus.runtime.annotations.StaticInitSafe
import jakarta.inject.Singleton
import org.eclipse.microprofile.config.inject.ConfigProperty
import tools.jackson.databind.json.JsonMapper

@Singleton
class RegisterCustomModuleCustomizer : JsonMapperBuilderCustomizer {
    @StaticInitSafe @ConfigProperty(name = "test.prop") lateinit var testProp: String

    override fun customize(builder: JsonMapper.Builder) {
        GreetingResource.MY_PROPERTY.set(testProp)
    }
}
