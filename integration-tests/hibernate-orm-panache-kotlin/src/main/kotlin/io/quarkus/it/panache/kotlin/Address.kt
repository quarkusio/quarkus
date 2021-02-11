package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
open class Address : PanacheEntityBase {
    companion object : PanacheCompanionBase<Address, Int> {
        override fun count(query: String, params: Map<String, Any>): Long {
            AddressDao.shouldBeOverridden()
        }
    }

    @Id
    @GeneratedValue
    @JvmField
    var id: Int? = null

    var street: String? = null

    var street2: String? = null

    constructor()

    constructor(street: String) {
        this.street = street
    }

    override fun toString() = "${javaClass.simpleName}<$id>"
}