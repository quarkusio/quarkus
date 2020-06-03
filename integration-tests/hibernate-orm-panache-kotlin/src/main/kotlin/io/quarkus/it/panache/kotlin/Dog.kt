package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
open class Dog() : PanacheEntity() {
    companion object : PanacheCompanion<Dog, Long>

    constructor(name: String, race: String): this() {
        this.name = name
        this.race = race
    }

    @ManyToOne
    var owner: Person? = null
    lateinit var name: String
    lateinit var race: String
}