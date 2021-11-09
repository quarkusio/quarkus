package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import javax.persistence.Entity


@Entity
open class Address : PanacheEntity {
    companion object : PanacheCompanion<Address> {
//        override fun count(query: String, params: Map<String, Any>): Long {
//            AddressDao.shouldBeOverridden()
//        }
    }

    var street: String? = null

    var street2: String? = null

    constructor()

    constructor(street: String) {
        this.street = street
    }

    override fun toString() = "${javaClass.simpleName}<$id>"
}