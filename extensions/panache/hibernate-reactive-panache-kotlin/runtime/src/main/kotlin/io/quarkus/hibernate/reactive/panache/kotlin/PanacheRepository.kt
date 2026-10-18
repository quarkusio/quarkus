package io.quarkus.hibernate.reactive.panache.kotlin

interface PanacheRepository<Entity : Any> : PanacheRepositoryBase<Entity, Long>
