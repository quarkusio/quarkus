package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase

abstract class AbstractRepository<T : Any> : PanacheRepositoryBase<T, String>
