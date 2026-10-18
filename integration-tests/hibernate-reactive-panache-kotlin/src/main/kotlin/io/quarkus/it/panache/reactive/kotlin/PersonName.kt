package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection data class PersonName(val uniqueName: String?, val name: String?)
