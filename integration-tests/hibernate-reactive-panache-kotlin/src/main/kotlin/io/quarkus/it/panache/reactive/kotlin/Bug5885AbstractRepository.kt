package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepository

abstract class Bug5885AbstractRepository<T : Any> : PanacheRepository<T>
