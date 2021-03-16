package io.quarkus.hibernate.orm.panache.kotlin

/**
 * Represents a Repository for a specific type of entity `Entity`, with an ID type of `Long`. Implementing this
 * interface will gain you the exact same useful methods that are on [PanacheEntity] and [PanacheCompanion]. If you
 * have a custom ID strategy, you should implement [PanacheRepositoryBase] instead.
 *
 * @param Entity The type of entity to operate on
 */
interface PanacheRepository<Entity : Any>: PanacheRepositoryBase<Entity, Long>