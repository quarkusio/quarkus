package io.quarkus.resteasy.mutiny.common.runtime;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

public interface UniRxInvoker extends RxInvoker<Uni<?>> {

    @Override
    Uni<Response> get();

    @Override
    <T> Uni<T> get(Class<T> responseType);

    @Override
    <T> Uni<T> get(GenericType<T> responseType);

    @Override
    Uni<Response> put(Entity<?> entity);

    @Override
    <T> Uni<T> put(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Uni<T> put(Entity<?> entity, GenericType<T> type);

    @Override
    Uni<Response> post(Entity<?> entity);

    @Override
    <T> Uni<T> post(Entity<?> entity, Class<T> clazz);

    @Override
    <T> Uni<T> post(Entity<?> entity, GenericType<T> type);

    @Override
    Uni<Response> delete();

    @Override
    <T> Uni<T> delete(Class<T> responseType);

    @Override
    <T> Uni<T> delete(GenericType<T> responseType);

    @Override
    Uni<Response> head();

    @Override
    Uni<Response> options();

    @Override
    <T> Uni<T> options(Class<T> responseType);

    @Override
    <T> Uni<T> options(GenericType<T> responseType);

    @Override
    Uni<Response> trace();

    @Override
    <T> Uni<T> trace(Class<T> responseType);

    @Override
    <T> Uni<T> trace(GenericType<T> responseType);

    @Override
    Uni<Response> method(String name);

    @Override
    <T> Uni<T> method(String name, Class<T> responseType);

    @Override
    <T> Uni<T> method(String name, GenericType<T> responseType);

    @Override
    Uni<Response> method(String name, Entity<?> entity);

    @Override
    <T> Uni<T> method(String name, Entity<?> entity, Class<T> responseType);

    @Override
    <T> Uni<T> method(String name, Entity<?> entity, GenericType<T> responseType);

}
