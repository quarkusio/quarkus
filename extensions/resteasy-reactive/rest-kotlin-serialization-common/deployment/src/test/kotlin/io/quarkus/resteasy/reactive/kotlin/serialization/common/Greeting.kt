package io.quarkus.resteasy.reactive.kotlin.serialization.common

import kotlinx.serialization.Serializable

@Serializable
data class Greeting(val name: String, val message: String)
