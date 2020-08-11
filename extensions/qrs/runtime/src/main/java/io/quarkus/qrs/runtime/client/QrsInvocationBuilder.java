package io.quarkus.qrs.runtime.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import io.quarkus.qrs.runtime.jaxrs.QrsConfiguration;
import io.quarkus.qrs.runtime.jaxrs.QrsResponseBuilder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

public class QrsInvocationBuilder implements Invocation.Builder {

    private String method = "GET";
    private final URI uri;
    private final HttpClient httpClient;
    private QrsWebTarget target;
    private boolean chunked;
    private final ClientRequestHeaders headers;

    public QrsInvocationBuilder(URI uri, HttpClient httpClient, QrsConfiguration configuration) {
        this.uri = uri;
        this.httpClient = httpClient;
        this.headers = new ClientRequestHeaders(configuration);
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
        return null;
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
        return null;
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        return null;
    }

    @Override
    public Response get() {
        return method("GET");
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return get().readEntity(responseType);
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response put(Entity<?> entity) {
        return null;
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response post(Entity<?> entity) {
        return null;
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response delete() {
        return method("DELETE");
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response head() {
        return null;
    }

    @Override
    public Response options() {
        return method("OPTIONS");
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response trace() {
        return null;
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response method(String name) {
        CompletableFuture<Response> result = new CompletableFuture<>();
        HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.valueOf(name), uri.getPort(), uri.getHost(),
                uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));
        for (Map.Entry<String, List<String>> entry : headers.asMap().entrySet()) {
            httpClientRequest.headers().add(entry.getKey(), entry.getValue());
        }
        httpClientRequest
                .handler(new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse event) {
                        event.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                QrsResponseBuilder response = new QrsResponseBuilder();
                                for (String i : event.headers().names()) {
                                    response.header(i, event.getHeader(i));
                                }
                                response.status(event.statusCode());
                                response.entity(buffer.toString(StandardCharsets.UTF_8));

                                result.complete(response.build());
                            }
                        });
                    }
                }).exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        result.completeExceptionally(event);
                    }
                }).end();

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return null;
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return null;
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return null;
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return null;
    }

    public void setTarget(QrsWebTarget target) {
        this.target = target;
    }

    public QrsWebTarget getTarget() {
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
