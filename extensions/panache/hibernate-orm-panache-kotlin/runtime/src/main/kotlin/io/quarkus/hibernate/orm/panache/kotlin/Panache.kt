package io.quarkus.hibernate.orm.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations

internal fun injectionMissing(): Nothing = throw JpaOperations.implementationInjectionMissing()
