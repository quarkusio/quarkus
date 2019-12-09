package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase

abstract class AbstractRepository<T: Any> : PanacheRepositoryBase<T, String>