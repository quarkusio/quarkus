package io.quarkus.resteasy.reactive.jackson.deployment.test

data class KotlinDefaultParamsDto(
    val name: String,
    val greeting: String = "Hello",
    val tags: List<String> = emptyList()
)
