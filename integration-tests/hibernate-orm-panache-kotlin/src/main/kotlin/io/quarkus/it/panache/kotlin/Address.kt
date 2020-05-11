package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.Entity

@Entity
open class Address : PanacheEntity, Comparable<Address> {
    companion object : PanacheCompanion<Address, Long> {
        override fun count(query: String, params: Map<String, Any>): Long {
            AddressDao.shouldBeOverridden()
        }
    }

    lateinit var street: String

    constructor()

    constructor(street: String) {
        this.street = street
    }

    override fun compareTo(other: Address): Int = street.compareTo(other.street)
}