package io.quarkus.rest.runtime.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.Serialisers;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;

public class QuarkusRestAsyncInvoker implements AsyncInvoker, CompletionStageRxInvoker {

    public static final Buffer EMPTY_BUFFER = Buffer.buffer(new byte[0]);

    final HttpClient httpClient;
    final URI uri;
    final Serialisers serialisers;
    final RequestSpec requestSpec;
    final Map<String, Object> properties;
    final QuarkusRestClient restClient;

    public QuarkusRestAsyncInvoker(QuarkusRestClient restClient, HttpClient httpClient, URI uri, Serialisers serialisers,
            RequestSpec requestSpec,
            Map<String, Object> properties) {
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.uri = uri;
        this.serialisers = serialisers;
        this.requestSpec = new RequestSpec(requestSpec);
        this.properties = new HashMap<>(properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public CompletableFuture<Response> get() {
        return performRequestInternal("GET", null, null);
    }

    @Override
    public <T> CompletableFuture<T> get(Class<T> responseType) {
        return mapResponse(performRequestInternal("GET", null, new GenericType<>(responseType)));
    }

    @Override
    public <T> CompletableFuture<T> get(GenericType<T> responseType) {
        return mapResponse(performRequestInternal("GET", null, responseType));
    }

    @Override
    public <T> CompletableFuture<T> get(InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> put(Entity<?> entity) {
        return performRequestInternal("PUT", entity, null);
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, Class<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("PUT", entity, new GenericType<>(responseType));
        return mapResponse(res);
    }

    public <T> CompletableFuture<T> mapResponse(CompletableFuture<Response> res) {
        return res.thenApply(new Function<Response, T>() {
            @Override
            public T apply(Response response) {
                return (T) response.getEntity();
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, GenericType<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("PUT", entity, responseType);
        return mapResponse(res);
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> post(Entity<?> entity) {
        return performRequestInternal("POST", entity, null);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, Class<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("POST", entity, new GenericType<>(responseType));
        return mapResponse(res);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, GenericType<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("POST", entity, responseType);
        return mapResponse(res);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> delete() {
        return performRequestInternal("DELETE", null, null);
    }

    @Override
    public <T> CompletableFuture<T> delete(Class<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("DELETE", null, new GenericType<>(responseType));
        return mapResponse(res);
    }

    @Override
    public <T> CompletableFuture<T> delete(GenericType<T> responseType) {
        CompletableFuture<Response> res = performRequestInternal("DELETE", null, responseType);
        return mapResponse(res);
    }

    @Override
    public <T> CompletableFuture<T> delete(InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> head() {
        return performRequestInternal("HEAD", null, null);
    }

    @Override
    public Future<Response> head(InvocationCallback<Response> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> options() {
        return performRequestInternal("OPTIONS", null, null);
    }

    @Override
    public <T> CompletableFuture<T> options(Class<T> responseType) {
        return mapResponse(performRequestInternal("OPTIONS", null, new GenericType<>(responseType)));
    }

    @Override
    public <T> CompletableFuture<T> options(GenericType<T> responseType) {
        return mapResponse(performRequestInternal("OPTIONS", null, responseType));
    }

    @Override
    public <T> CompletableFuture<T> options(InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> trace() {
        return performRequestInternal("TRACE", null, null);
    }

    @Override
    public <T> CompletableFuture<T> trace(Class<T> responseType) {
        return mapResponse(performRequestInternal("TRACE", null, new GenericType<>(responseType)));
    }

    @Override
    public <T> CompletableFuture<T> trace(GenericType<T> responseType) {
        return mapResponse(performRequestInternal("TRACE", null, responseType));
    }

    @Override
    public <T> CompletableFuture<T> trace(InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> method(String name) {
        return performRequestInternal(name, null, null);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Class<T> responseType) {
        return mapResponse(performRequestInternal(name, null, new GenericType<>(responseType)));
    }

    @Override
    public <T> CompletableFuture<T> method(String name, GenericType<T> responseType) {
        return mapResponse(performRequestInternal(name, null, responseType));
    }

    @Override
    public <T> CompletableFuture<T> method(String name, InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @Override
    public CompletableFuture<Response> method(String name, Entity<?> entity) {
        return performRequestInternal(name, entity, null);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, Class<T> responseType) {
        CompletableFuture<Response> response = performRequestInternal(name, entity, new GenericType<>(responseType));
        return mapResponse(response);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        CompletableFuture<Response> response = performRequestInternal(name, entity, responseType);
        return mapResponse(response);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
        throw new RuntimeException("NYI");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> CompletableFuture<Response> performRequestInternal(String httpMethodName, Entity<?> entity,
            GenericType<?> responseType) {
        return (CompletableFuture) performRequestInternal(httpMethodName, entity, responseType, true).getResult();
    }

    <T> InvocationState performRequestInternal(String httpMethodName, Entity<?> entity, GenericType<?> responseType,
            boolean registerBodyHandler) {
        return new InvocationState(restClient, httpClient, httpMethodName, uri,
                requestSpec.headers, serialisers,
                entity, responseType, registerBodyHandler);
    }
}
