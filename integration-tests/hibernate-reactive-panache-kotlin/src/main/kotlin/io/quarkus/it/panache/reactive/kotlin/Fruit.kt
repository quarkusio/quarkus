package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class Fruit : PanacheEntity {
    var name: String? = null
    var color: String? = null

    companion object : PanacheCompanion<Fruit>

    constructor(name: String?, color: String?) {
        this.name = name
        this.color = color
    }

    constructor() {}
}
