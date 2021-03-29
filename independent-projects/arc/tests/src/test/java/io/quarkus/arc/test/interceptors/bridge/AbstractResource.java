package io.quarkus.arc.test.interceptors.bridge;

public abstract class AbstractResource<T> {

    public String create(T dto) {
        return dto.toString();
    }
}
