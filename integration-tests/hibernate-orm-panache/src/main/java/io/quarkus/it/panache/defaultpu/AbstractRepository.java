package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public class AbstractRepository<T> implements PanacheRepositoryBase<T, String> {
}
