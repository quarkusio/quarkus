package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase

abstract class AbstractRepository<T: Any> : PanacheMongoRepositoryBase<T, String>