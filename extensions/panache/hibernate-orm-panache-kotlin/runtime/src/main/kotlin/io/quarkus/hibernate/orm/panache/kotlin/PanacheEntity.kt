package io.quarkus.hibernate.orm.panache.kotlin


import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * Represents an entity with a generated ID field [id] of type [Long]. If your
 * Hibernate entities extend this class they gain the ID field and auto-generated accessors
 * to all their public fields (unless annotated with [Transient]), as well as all
 * the useful methods from [PanacheEntityBase].
 * <p>
 * If you want a custom ID type or strategy, you can directly extend [PanacheEntityBase]
 * instead, and write your own ID field. You will still get auto-generated accessors and
 * all the useful methods.
 *
 * @see PanacheEntityBase
 */
@MappedSuperclass
open class PanacheEntity: PanacheEntityBase {
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
