package io.quarkus.it.panache;

import jakarta.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@MappedSuperclass
public abstract class GenericEntity<T> extends PanacheEntity {
    public T t;
    public T t2;

    public T getT2() {
        return t2;
    }

    public void setT2(T t2) {
        this.t2 = t2;
    }
}
