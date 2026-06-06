package io.quarkus.it.panache.kotlin

import io.quarkus.runtime.annotations.RegisterForReflection

@JvmInline value class GreetingId(val value: Long)

@RegisterForReflection
data class GreetingValueClassDto(val id: GreetingId? = null, val name: String)
