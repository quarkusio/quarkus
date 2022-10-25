package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
open class Dog() : PanacheEntityBase {
    companion object : PanacheCompanion<Dog>

    constructor(name: String, race: String) : this() {
        this.name = name
        this.race = race
    }

    @ManyToOne
    var owner: Person? = null
    var name: String? = null
    var race: String? = null

    /**
     * The auto-generated ID field. This field is set by Hibernate ORM when this entity
     * is persisted.
     *
     * @see [PanacheEntity.persist]
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    /**
     * Default toString() implementation
     *
     * @return the class type and ID type
     */
    override fun toString() = "${javaClass.simpleName}<$id>"
}
