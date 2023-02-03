package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Transient
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlTransient
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.hibernate.annotations.ParamDef

@XmlRootElement
@Entity(name = "Person2")
@FilterDefs(
    FilterDef(name = "Person.hasName", defaultCondition = "name = :name", parameters = [ParamDef(name = "name", type = "string")]),
    FilterDef(name = "Person.isAlive", defaultCondition = "status = 'LIVING'")
)
@Filters(Filter(name = "Person.isAlive"), Filter(name = "Person.hasName"))
open class Person : PanacheEntity() {
    companion object : PanacheCompanion<Person> {
        fun findOrdered(): List<Address> = AddressDao.shouldBeOverridden()
    }

    var name: String? = null

    @Column(unique = true)
    var uniqueName: String? = null

    @ManyToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var address: Address? = null

    // FIXME: this isn't working
    @Enumerated(EnumType.STRING)
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

    override fun toString(): String {
        return "Person(id=$id, name=$name, status=$status)"
    }
}
