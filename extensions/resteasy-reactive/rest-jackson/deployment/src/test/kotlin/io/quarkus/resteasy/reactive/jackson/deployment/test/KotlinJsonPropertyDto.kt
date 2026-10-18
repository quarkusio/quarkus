package io.quarkus.resteasy.reactive.jackson.deployment.test

import com.fasterxml.jackson.annotation.JsonProperty

data class KotlinJsonPropertyDto(
    @JsonProperty("name")
    val field: String
)
