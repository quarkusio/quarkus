package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
class CatProjectionBean
@JvmOverloads
constructor(val name: String, val ownerName: String, val weight: Double? = null)
