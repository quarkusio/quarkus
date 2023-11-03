package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.core.GenericType;

public abstract class AbstractRxInvoker<T> implements RxInvoker<T> {

    @Override
    public T get() {
        return method("GET");
    }

    @Override
    public <R> T get(Class<R> responseType) {
        return method("GET", responseType);
    }

    @Override
    public <R> T get(GenericType<R> responseType) {
        return method("GET", responseType);
    }

    @Override
    public T put(Entity<?> entity) {
        return method("PUT");
    }

    @Override
    public <R> T put(Entity<?> entity, Class<R> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public <R> T put(Entity<?> entity, GenericType<R> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public T post(Entity<?> entity) {
        return method("POST");
    }

    @Override
    public <R> T post(Entity<?> entity, Class<R> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public <R> T post(Entity<?> entity, GenericType<R> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public T delete() {
        return method("DELETE");
    }

    @Override
    public <R> T delete(Class<R> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public <R> T delete(GenericType<R> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public T head() {
        return method("HEAD");
    }

    @Override
    public T options() {
        return method("OPTIONS");
    }

    @Override
    public <R> T options(Class<R> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public <R> T options(GenericType<R> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public T trace() {
        return method("TRACE");
    }

    @Override
    public <R> T trace(Class<R> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <R> T trace(GenericType<R> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public T method(String name) {
        return method(name, (GenericType<?>) null);
    }

    @Override
    public <R> T method(String name, Class<R> responseType) {
        return method(name, new GenericType<>(responseType));
    }

    @Override
    public <R> T method(String name, GenericType<R> responseType) {
        return method(name, null, responseType);
    }

    @Override
    public T method(String name, Entity<?> entity) {
        return method(name, entity, (GenericType<?>) null);
    }

    @Override
    public <R> T method(String name, Entity<?> entity, Class<R> responseType) {
        return method(name, entity, new GenericType<>(responseType));
    }
}
