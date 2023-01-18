package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
open class Bug7721EntitySuperClass : PanacheEntity() {
    @JvmField
    @Column(nullable = false)
    var superField: String = "default"
}
