package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.NamedQuery

@MappedSuperclass
@NamedQuery(name = "NamedQueryMappedSuperClass.getAll", query = "from NamedQueryEntity")
open class NamedQueryMappedSuperClass : PanacheEntity() {
    lateinit var superField: String
}
