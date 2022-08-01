package org.jboss.resteasy.reactive.client.impl;

import io.smallrye.mutiny.Uni;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class UniInvoker extends AbstractRxInvoker<Uni<?>> {

    private InvocationBuilderImpl invocationBuilder;

    public UniInvoker(InvocationBuilderImpl invocationBuilder) {
        this.invocationBuilder = invocationBuilder;
    }

    @Override
    public <R> Uni<R> method(String name, Entity<?> entity, GenericType<R> responseType) {
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) invocationBuilder.rx();
        return Uni.createFrom().completionStage(new Supplier<CompletionStage<R>>() {
            @Override
            public CompletionStage<R> get() {
                return invoker.method(name, entity, responseType);
            }
        });
    }

    @Override
    public Uni<Response> get() {
        return (Uni<Response>) super.get();
    }

    @Override
    public <T> Uni<T> get(Class<T> responseType) {
        return (Uni<T>) super.get(responseType);
    }

    @Override
    public <T> Uni<T> get(GenericType<T> responseType) {
        return (Uni<T>) super.get(responseType);
    }

    @Override
    public Uni<Response> put(Entity<?> entity) {
        return (Uni<Response>) super.put(entity);
    }

    @Override
    public <T> Uni<T> put(Entity<?> entity, Class<T> clazz) {
        return (Uni<T>) super.put(entity, clazz);
    }

    @Override
    public <T> Uni<T> put(Entity<?> entity, GenericType<T> type) {
        return (Uni<T>) super.put(entity, type);
    }

    @Override
    public Uni<Response> post(Entity<?> entity) {
        return (Uni<Response>) super.post(entity);
    }

    @Override
    public <T> Uni<T> post(Entity<?> entity, Class<T> clazz) {
        return (Uni<T>) super.post(entity, clazz);
    }

    @Override
    public <T> Uni<T> post(Entity<?> entity, GenericType<T> type) {
        return (Uni<T>) super.post(entity, type);
    }

    @Override
    public Uni<Response> delete() {
        return (Uni<Response>) super.delete();
    }

    @Override
    public <T> Uni<T> delete(Class<T> responseType) {
        return (Uni<T>) super.delete(responseType);
    }

    @Override
    public <T> Uni<T> delete(GenericType<T> responseType) {
        return (Uni<T>) super.delete(responseType);
    }

    @Override
    public Uni<Response> head() {
        return (Uni<Response>) super.head();
    }

    @Override
    public Uni<Response> options() {
        return (Uni<Response>) super.options();
    }

    @Override
    public <T> Uni<T> options(Class<T> responseType) {
        return (Uni<T>) super.options(responseType);
    }

    @Override
    public <T> Uni<T> options(GenericType<T> responseType) {
        return (Uni<T>) super.options(responseType);
    }

    @Override
    public Uni<Response> trace() {
        return (Uni<Response>) super.trace();
    }

    @Override
    public <T> Uni<T> trace(Class<T> responseType) {
        return (Uni<T>) super.trace(responseType);
    }

    @Override
    public <T> Uni<T> trace(GenericType<T> responseType) {
        return (Uni<T>) super.trace(responseType);
    }

    @Override
    public Uni<Response> method(String name) {
        return (Uni<Response>) super.method(name);
    }

    @Override
    public <T> Uni<T> method(String name, Class<T> responseType) {
        return (Uni<T>) super.method(name, responseType);
    }

    @Override
    public <T> Uni<T> method(String name, GenericType<T> responseType) {
        return (Uni<T>) super.method(name, responseType);
    }

    @Override
    public Uni<Response> method(String name, Entity<?> entity) {
        return (Uni<Response>) super.method(name, entity);
    }

    @Override
    public <T> Uni<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return (Uni<T>) super.method(name, entity, responseType);
    }
}
