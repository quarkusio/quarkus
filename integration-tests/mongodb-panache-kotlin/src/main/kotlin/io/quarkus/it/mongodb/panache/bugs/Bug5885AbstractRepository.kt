package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase

abstract class Bug5885AbstractRepository<T: Any> : PanacheMongoRepositoryBase<T, Long>