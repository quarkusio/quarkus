package io.quarkus.hibernate.orm.panache.kotlin


import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
open class PanacheEntity: PanacheEntityBase {
    @Id
    @GeneratedValue
    @JvmField
    var id: Long? = null

    override fun toString() = this.javaClass.simpleName + "<" + id + ">"
}
