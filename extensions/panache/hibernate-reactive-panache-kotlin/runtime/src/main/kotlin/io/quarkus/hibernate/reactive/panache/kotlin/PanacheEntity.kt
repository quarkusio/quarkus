package io.quarkus.hibernate.reactive.panache.kotlin

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
open class PanacheEntity : PanacheEntityBase {
    /**
     * The auto-generated ID field. This field is set by Hibernate ORM when this entity
     * is persisted.
     *
     * @see [PanacheEntity.persist]
     */
    @Id
    @GeneratedValue
    open var id: Long? = null

    /**
     * Default toString() implementation
     *
     * @return the class type and ID type
     */
    override fun toString() = "${javaClass.simpleName}<$id>"
}
