package io.quarkus.hibernate.reactive.panache.kotlin

import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

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
