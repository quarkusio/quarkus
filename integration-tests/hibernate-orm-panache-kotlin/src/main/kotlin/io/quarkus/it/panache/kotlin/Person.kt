package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Transient
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlTransient

@XmlRootElement
@Entity(name = "Person2")
open class Person : PanacheEntity() {
    companion object : PanacheCompanion<Person, Long> {
        fun findOrdered(): List<Dog>  = AddressDao.shouldBeOverridden()
    }

    var name: String? = null
    @Column(unique = true)
    var uniqueName: String? = null
    @ManyToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var address: Address? = null
    var status: Status? = null
    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var dogs = mutableListOf<Dog>()

    @XmlTransient
    @Transient
    var serialisationTrick = 0
        get() {
            return ++field
        }
        set(value) {
            field = value
        }
}