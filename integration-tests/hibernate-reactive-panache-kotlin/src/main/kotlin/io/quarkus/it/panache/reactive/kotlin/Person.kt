package io.quarkus.it.panache.reactive.kotlin

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.KotlinJpaOperations.Companion.INSTANCE
import io.smallrye.mutiny.Uni
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.OneToMany
import jakarta.persistence.Transient
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlTransient
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef

@XmlRootElement
@NamedQueries(
    NamedQuery(name = "Person.getByName", query = "from Person2 where name = :name"),
    NamedQuery(name = "Person.countAll", query = "select count(*) from Person2"),
    NamedQuery(name = "Person.countByName", query = "select count(*) from Person2 where name = :name"),
    NamedQuery(name = "Person.countByName.ordinal", query = "select count(*) from Person2 where name = ?1"),
    NamedQuery(name = "Person.updateAllNames", query = "Update Person2 p set p.name = :name"),
    NamedQuery(name = "Person.updateNameById", query = "Update Person2 p set p.name = :name where p.id = :id"),
    NamedQuery(name = "Person.updateNameById.ordinal", query = "Update Person2 p set p.name = ?1 where p.id = ?2"),
    NamedQuery(name = "Person.deleteAll", query = "delete from Person2"),
    NamedQuery(name = "Person.deleteById", query = "delete from Person2 p where p.id = :id"),
    NamedQuery(name = "Person.deleteById.ordinal", query = "delete from Person2 p where p.id = ?1")
)
@FilterDef(
    name = "Person.hasName",
    defaultCondition = "name = :name",
    parameters = [ParamDef(name = "name", type = "string")]
)
@Filter(name = "Person.isAlive")
@Filter(name = "Person.hasName")
@FilterDef(name = "Person.isAlive", defaultCondition = "status = 'LIVING'")
@Entity(name = "Person2")
class Person : PanacheEntity() {
    companion object : PanacheCompanion<Person> {
        fun findOrdered(): Uni<List<Person>> {
            return find("ORDER BY name").list()
        }

        // For https://github.com/quarkusio/quarkus/issues/9635
        @Suppress("UNCHECKED_CAST")
        override fun find(query: String, vararg params: Any): PanacheQuery<Person> {
            return INSTANCE.find(Person::class.java, query, *params) as PanacheQuery<Person>
        }

        @Suppress("UNUSED_PARAMETER")
        fun methodWithPrimitiveParams(b: Boolean, bb: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char) = 0
    }

    var name: String? = null

    @Column(unique = true)
    var uniqueName: String? = null

    @ManyToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var address: Address? = null

    @Enumerated(EnumType.STRING)
    var status: Status? = null

    @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var dogs = mutableListOf<Dog>()

    // note that this annotation is automatically added for mapped fields, which is not the case here
    // so we do it manually to emulate a mapped field situation
    @Transient
    @XmlTransient
    var serialisationTrick = 0
        @JsonProperty
        get() = ++field
}
