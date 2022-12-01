package io.quarkus.it.mongodb.panache;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

public abstract class GenericEntity<T> extends PanacheMongoEntity {
    public T t;
    public T t2;

    public T getT2() {
        return t2;
    }

    public void setT2(T t2) {
        this.t2 = t2;
    }
}