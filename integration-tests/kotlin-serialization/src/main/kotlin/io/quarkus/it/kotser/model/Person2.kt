package io.quarkus.it.kotser.model

import kotlinx.serialization.Serializable

@Serializable
data class Person2(var fullName: String, var defaulted: String = "hey") {
    override fun toString(): String {
        TODO("this shouldn't get called. a proper serialization should be invoked.")
    }
}
