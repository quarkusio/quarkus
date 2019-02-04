package org.jboss.panache.rx;

import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Single;

public interface RxModelInfo<T extends RxEntityBase<?>> {
    Class<T> getEntityClass();
    String getTableName();
    T fromRow(Row row);
    String insertStatement();
    String updateStatement();
    Single<Tuple> toTuple(T entity);
}