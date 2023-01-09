package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity

@Entity
class Address : PanacheEntity, Comparable<Address> {
    companion object : PanacheCompanion<Address>

    var street: String? = null

    constructor() {}
    constructor(street: String) {
        this.street = street
    }

    override operator fun compareTo(address: Address): Int {
        return street!!.compareTo(address.street!!)
    }
}
