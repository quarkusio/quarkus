package org.jboss.resteasy.reactive.client.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientRequest;

public class UniInvoker extends AbstractRxInvoker<Uni<?>> {

    private final InvocationBuilderImpl invocationBuilder;

    public UniInvoker(InvocationBuilderImpl invocationBuilder) {
        this.invocationBuilder = invocationBuilder;
    }

    @Override
    public <R> Uni<R> method(String name, Entity<?> entity, GenericType<R> responseType) {
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) invocationBuilder.rx();
        AtomicReference<RestClientRequestContext> restClientRequestContextRef = new AtomicReference<>();
        return Uni.createFrom().completionStage(new Supplier<CompletionStage<R>>() {
            @Override
            public CompletionStage<R> get() {
                RestClientRequestContext restClientRequestContext = invoker.performRequestInternal(name, entity,
                        responseType == null ? new GenericType<>(String.class) : responseType,
                        true);
                restClientRequestContextRef.set(restClientRequestContext);
                CompletableFuture response = restClientRequestContext.getResult();
                return invoker.mapResponse(response, responseType == null ? String.class : responseType.getRawType());
            }
        }).onFailure().transform(new Function<>() {
            @Override
            public Throwable apply(Throwable t) {
                if ((t instanceof ProcessingException) && (t.getCause() != null)) {
                    return t.getCause();
                }
                return t;
            }
        }).onCancellation().invoke(new Runnable() {
            @Override
            public void run() {
                // be very defensive here as things could have been nulled out when the application is being torn down
                RestClientRequestContext restClientRequestContext = restClientRequestContextRef.get();
                if (restClientRequestContext != null) {
                    restClientRequestContext.setUserCanceled();

                    HttpClientRequest httpClientRequest = restClientRequestContext.getHttpClientRequest();
                    if (httpClientRequest != null) {
                        // if there is already an HTTP request in flight, cancel it
                        httpClientRequest.reset();
                    } else {
                        // by having already done setUserCanceled, Quarkus knows to reset the request when it finally gets created
                    }
                }
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
