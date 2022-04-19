package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class Dog() : PanacheEntityBase {
    constructor(name: String, race: String) : this() {
        this.name = name
        this.race = race
    }

    @Id
    @GeneratedValue
    var id: Int? = null

    lateinit var name: String

    lateinit var race: String

    @ManyToOne
    lateinit var owner: Person

    companion object : PanacheCompanionBase<Dog, Int>
}
