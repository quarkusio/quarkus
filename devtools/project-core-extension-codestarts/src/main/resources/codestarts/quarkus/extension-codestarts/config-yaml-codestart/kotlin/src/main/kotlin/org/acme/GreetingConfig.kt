package org.acme

import io.quarkus.arc.config.ConfigProperties
import org.eclipse.microprofile.config.inject.ConfigProperty

@ConfigProperties(prefix = "greeting")
interface GreetingConfig {

    @ConfigProperty(name = "message")
    fun message(): String?

}