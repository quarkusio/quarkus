package io.quarkus.it.kotser.model

import io.quarkus.runtime.annotations.RegisterForReflection
import kotlinx.serialization.Serializable

@Serializable
@RegisterForReflection
data class Person(var name: String, var defaulted: String = "hi there!") {
    override fun toString(): String {
        TODO("this shouldn't get called. a proper serialization should be invoked.")
    }
}
