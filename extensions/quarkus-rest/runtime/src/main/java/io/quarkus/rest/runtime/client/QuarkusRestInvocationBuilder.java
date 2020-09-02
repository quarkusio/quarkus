package io.quarkus.rest.runtime.client;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.vertx.core.http.HttpClient;

public class QuarkusRestInvocationBuilder implements Invocation.Builder {

    String method = "GET";
    final URI uri;
    final HttpClient httpClient;
    QuarkusRestWebTarget target;
    boolean chunked;
    final ClientRequestHeaders headers;
    final Serialisers serialisers;
    final QuarkusRestAsyncInvoker invoker;

    public QuarkusRestInvocationBuilder(URI uri, HttpClient httpClient, QuarkusRestConfiguration configuration,
            Serialisers serialisers) {
        this.uri = uri;
        this.httpClient = httpClient;
        this.headers = new ClientRequestHeaders(configuration);
        this.serialisers = serialisers;
        this.invoker = new QuarkusRestAsyncInvoker(this);
    }

    @Override
    public Invocation build(String method) {
        return null;
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return null;
    }

    @Override
    public Invocation buildGet() {
        return null;
    }

    @Override
    public Invocation buildDelete() {
        return null;
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return null;
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return null;
    }

    @Override
    public AsyncInvoker async() {
        return invoker;
    }

    @Override
    public Invocation.Builder accept(String... mediaTypes) {
        return null;
    }

    @Override
    public Invocation.Builder accept(MediaType... mediaTypes) {
        return null;
    }

    @Override
    public Invocation.Builder acceptLanguage(Locale... locales) {
        return null;
    }

    @Override
    public Invocation.Builder acceptLanguage(String... locales) {
        return null;
    }

    @Override
    public Invocation.Builder acceptEncoding(String... encodings) {
        return null;
    }

    @Override
    public Invocation.Builder cookie(Cookie cookie) {
        return null;
    }

    @Override
    public Invocation.Builder cookie(String name, String value) {
        return null;
    }

    @Override
    public Invocation.Builder cacheControl(CacheControl cacheControl) {
        return null;
    }

    @Override
    public Invocation.Builder header(String name, Object value) {
        headers.header(name, value);
        return this;
    }

    @Override
    public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
        return null;
    }

    @Override
    public Invocation.Builder property(String name, Object value) {
        return null;
    }

    @Override
    public CompletionStageRxInvoker rx() {
        return invoker;
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        if (clazz == QuarkusRestMultiInvoker.class) {
            return (T) new QuarkusRestMultiInvoker(target);
        }
        return null;
    }

    @Override
    public Response get() {
        return unwrap(invoker.get());
    }

    private <T> T unwrap(CompletableFuture<T> c) {
        try {
            return c.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return unwrap(invoker.get(responseType));
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return unwrap(invoker.get(responseType));
    }

    @Override
    public Response put(Entity<?> entity) {
        return unwrap(invoker.put(entity));
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return unwrap(invoker.put(entity, responseType));
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return unwrap(invoker.put(entity, responseType));
    }

    @Override
    public Response post(Entity<?> entity) {
        return unwrap(invoker.post(entity));
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return unwrap(invoker.post(entity, responseType));
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return unwrap(invoker.post(entity, responseType));
    }

    @Override
    public Response delete() {
        return unwrap(invoker.delete());
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return unwrap(invoker.delete(responseType));
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return unwrap(invoker.delete(responseType));
    }

    @Override
    public Response head() {
        return unwrap(invoker.head());
    }

    @Override
    public Response options() {
        return unwrap(invoker.options());
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return unwrap(invoker.options(responseType));
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return unwrap(invoker.options(responseType));
    }

    @Override
    public Response trace() {
        return unwrap(invoker.trace());
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return unwrap(invoker.trace(responseType));
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return unwrap(invoker.trace(responseType));
    }

    @Override
    public Response method(String name) {
        return unwrap(invoker.method(name));
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return unwrap(invoker.method(name, responseType));
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return unwrap(invoker.method(name, responseType));
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return unwrap(invoker.method(name, entity));
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return unwrap(invoker.method(name, entity, responseType));
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return unwrap(invoker.method(name, entity, responseType));
    }

    public void setTarget(QuarkusRestWebTarget target) {
        this.target = target;
    }

    public QuarkusRestWebTarget getTarget() {
        return target;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean getChunked() {
        return chunked;
    }

    public ClientRequestHeaders getHeaders() {
        return headers;
    }

}
