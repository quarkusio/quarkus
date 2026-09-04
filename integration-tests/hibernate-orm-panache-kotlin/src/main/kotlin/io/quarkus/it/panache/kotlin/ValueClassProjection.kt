package io.quarkus.it.panache.kotlin

import io.quarkus.runtime.annotations.RegisterForReflection

// Hibernate instantiates the value class itself through the generated `new GreetingId(id)`
// constructor, so it needs its own reflection metadata in native mode.
@RegisterForReflection @JvmInline value class GreetingId(val value: Long)

@RegisterForReflection
data class GreetingValueClassDto(val id: GreetingId? = null, val name: String)
