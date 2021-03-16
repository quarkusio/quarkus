package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository

abstract class Bug6324AbstractRepository<T: Any> : PanacheMongoRepository<T>