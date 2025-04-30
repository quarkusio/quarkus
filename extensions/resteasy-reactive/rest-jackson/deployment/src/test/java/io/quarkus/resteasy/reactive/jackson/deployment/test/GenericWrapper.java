package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class GenericWrapper<T> {

    public String name;

    public T entity;

    public GenericWrapper(String name, T entity) {
        this.name = name;
        this.entity = entity;
    }

}
