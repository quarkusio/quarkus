package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class GenericEntity<T> : PanacheEntity() {
    var t: T? = null
    var t2: T? = null
}