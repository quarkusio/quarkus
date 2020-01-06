package io.quarkus.resteasy.mutiny.runtime;

import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

public class UniRxInvokerImpl implements UniRxInvoker {
    private final CompletionStageRxInvoker completionStageRxInvoker;
    private final UniProvider UniProvider;

    public UniRxInvokerImpl(final CompletionStageRxInvoker completionStageRxInvoker) {
        this.completionStageRxInvoker = completionStageRxInvoker;
        this.UniProvider = new UniProvider();
    }

    @Override
    public Uni<Response> get() {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.get());
    }

    @Override
    public <T> Uni<T> get(Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.get(responseType));
    }

    @Override
    public <T> Uni<T> get(GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.get(responseType));
    }

    @Override
    public Uni<Response> put(Entity<?> entity) {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.put(entity));
    }

    @Override
    public <T> Uni<T> put(Entity<?> entity, Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.put(entity, responseType));
    }

    @Override
    public <T> Uni<T> put(Entity<?> entity, GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.put(entity, responseType));
    }

    @Override
    public Uni<Response> post(Entity<?> entity) {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.post(entity));
    }

    @Override
    public <T> Uni<T> post(Entity<?> entity, Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.post(entity, responseType));
    }

    @Override
    public <T> Uni<T> post(Entity<?> entity, GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.post(entity, responseType));
    }

    @Override
    public Uni<Response> delete() {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.delete());
    }

    @Override
    public <T> Uni<T> delete(Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.delete(responseType));
    }

    @Override
    public <T> Uni<T> delete(GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.delete(responseType));
    }

    @Override
    public Uni<Response> head() {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.head());
    }

    @Override
    public Uni<Response> options() {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.options());
    }

    @Override
    public <T> Uni<T> options(Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.options(responseType));
    }

    @Override
    public <T> Uni<T> options(GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.options(responseType));
    }

    @Override
    public Uni<Response> trace() {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.trace());
    }

    @Override
    public <T> Uni<T> trace(Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.trace(responseType));
    }

    @Override
    public <T> Uni<T> trace(GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.trace(responseType));
    }

    @Override
    public Uni<Response> method(String name) {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name));
    }

    @Override
    public <T> Uni<T> method(String name, Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name, responseType));
    }

    @Override
    public <T> Uni<T> method(String name, GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name, responseType));
    }

    @Override
    public Uni<Response> method(String name, Entity<?> entity) {
        return (Uni<Response>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name, entity));
    }

    @Override
    public <T> Uni<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name, entity, responseType));
    }

    @Override
    public <T> Uni<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return (Uni<T>) UniProvider.fromCompletionStage(completionStageRxInvoker.method(name, entity, responseType));
    }
}
