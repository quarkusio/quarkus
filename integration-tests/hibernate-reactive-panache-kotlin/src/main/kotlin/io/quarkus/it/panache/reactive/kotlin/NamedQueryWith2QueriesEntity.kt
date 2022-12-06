package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.NamedQuery

@Entity
@NamedQuery(name = "NamedQueryWith2QueriesEntity.getAll1", query = "from NamedQueryWith2QueriesEntity")
@NamedQuery(name = "NamedQueryWith2QueriesEntity.getAll2", query = "from NamedQueryWith2QueriesEntity")
class NamedQueryWith2QueriesEntity : PanacheEntity() {
    lateinit var test: String

    companion object : PanacheCompanion<NamedQueryWith2QueriesEntity>
}
