package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository

abstract class Bug5885AbstractRepository<Entity : Any> : PanacheRepository<Entity>