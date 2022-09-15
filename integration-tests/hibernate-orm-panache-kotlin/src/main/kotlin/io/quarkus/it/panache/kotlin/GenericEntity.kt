package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class GenericEntity<T> : PanacheEntity() {
    var t: T? = null
    var t2: T? = null
}
