package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class CatOwner @JvmOverloads constructor(var name: String? = null) : PanacheEntity() {
    companion object : PanacheCompanion<CatOwner>
}
