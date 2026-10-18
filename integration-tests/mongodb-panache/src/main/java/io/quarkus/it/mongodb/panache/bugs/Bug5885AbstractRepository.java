package io.quarkus.it.mongodb.panache.bugs;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;

public abstract class Bug5885AbstractRepository<T> implements PanacheMongoRepositoryBase<T, Long> {

}
