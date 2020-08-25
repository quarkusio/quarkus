package io.quarkus.qrs.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.jaxrs.QrsResponseBuilder;
import io.quarkus.qrs.runtime.util.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

public class QrsAsyncInvoker implements AsyncInvoker, CompletionStageRxInvoker {
    public static final Buffer EMPTY_BUFFER = Buffer.buffer(new byte[0]);
    final QrsInvocationBuilder builder;

    public QrsAsyncInvoker(QrsInvocationBuilder builder) {
        this.builder = builder;
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

    private <T> CompletableFuture<Response> performRequestInternal(String httpMethodName, Entity<?> entity, GenericType<T> rt) {
        return performRequestInternal(httpMethodName, entity, rt, null);
    }

    @SuppressWarnings("deprecation")
    <T> CompletableFuture<Response> performRequestInternal(String httpMethodName, Entity<?> entity, GenericType<T> rt,
            Consumer<HttpClientResponse> clientConsumer) {

        CompletableFuture<Response> result = new CompletableFuture<>();
        try {
            GenericType<T> responseType = getResponseType(rt);
            HttpClientRequest httpClientRequest = createRequest(httpMethodName);
            Buffer actualEntity = setRequestHeadersAndPrepareBody(httpClientRequest, entity, responseType);
            httpClientRequest
                    .handler(new Handler<HttpClientResponse>() {
                        @Override
                        public void handle(HttpClientResponse event) {
                            QrsResponseBuilder response;
                            MediaType mediaType;
                            try {
                                response = new QrsResponseBuilder();
                                mediaType = initialiseResponse(event, response);
                            } catch (Throwable t) {
                                result.completeExceptionally(t);
                                return;
                            }
                            if (clientConsumer != null) {
                                clientConsumer.accept(event);
                                result.complete(response.build());
                            } else {
                                event.bodyHandler(new Handler<Buffer>() {
                                    @Override
                                    public void handle(Buffer buffer) {
                                        try {
                                            readEntity(event, response, buffer, responseType, mediaType);
                                            result.complete(response.build());
                                        } catch (Throwable t) {
                                            result.completeExceptionally(t);
                                        }
                                    }
                                });
                            }
                        }
                    }).exceptionHandler(new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable event) {
                            result.completeExceptionally(event);
                        }
                    }).end(actualEntity);
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }

        return result;
    }

    private <T> void readEntity(HttpClientResponse event, QrsResponseBuilder response, Buffer buffer,
            GenericType<T> responseType, MediaType mediaType)
            throws IOException {
        List<MessageBodyReader<?>> readers = builder.serialisers.findReaders(responseType.getRawType(),
                mediaType);
        for (MessageBodyReader reader : readers) {
            if (reader.isReadable(responseType.getRawType(), responseType.getType(), null,
                    mediaType)) {
                ByteArrayInputStream in = new ByteArrayInputStream(buffer.getBytes());
                response.entity(
                        reader.readFrom(responseType.getRawType(), responseType.getType(),
                                null, mediaType, response.getMetadata(), in));
                return;
            }
        }

        response.entity(buffer.toString(StandardCharsets.UTF_8));
    }

    private MediaType initialiseResponse(HttpClientResponse event, QrsResponseBuilder response) {
        MediaType mediaType = MediaType.WILDCARD_TYPE;
        for (String i : event.headers().names()) {
            response.header(i, event.getHeader(i));

        }
        String mediaTypeHeader = event.getHeader(HttpHeaderNames.CONTENT_TYPE);
        if (mediaTypeHeader != null) {
            mediaType = MediaType.valueOf(mediaTypeHeader);
        }
        response.status(event.statusCode());
        return mediaType;
    }

    private <T> GenericType<T> getResponseType(GenericType<T> rt) {
        if (rt == null) {
            return new GenericType<>(String.class);
        }
        return rt;
    }

    private <T> Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest, Entity<?> entity, GenericType<T> rt)
            throws IOException {
        ClientRequestHeaders headers = builder.headers;
        MultivaluedMap<String, String> headerMap = headers.asMap();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            httpClientRequest.headers().add(entry.getKey(), entry.getValue());
        }
        if (entity != null && entity.getMediaType() != null) {
            httpClientRequest.headers().set(HttpHeaders.CONTENT_TYPE, entity.getMediaType().toString());
        }
        Buffer actualEntity = EMPTY_BUFFER;
        if (entity != null) {

            Class<?> entityType = entity.getEntity().getClass();
            List<MessageBodyWriter<?>> writers = builder.serialisers.findWriters(entityType, entity.getMediaType());
            for (MessageBodyWriter writer : writers) {
                if (writer.isWriteable(entityType, entityType, entity.getAnnotations(), entity.getMediaType())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writer.writeTo(entity.getEntity(), entityType, entityType, entity.getAnnotations(),
                            entity.getMediaType(), headerMap, baos);
                    actualEntity = Buffer.buffer(baos.toByteArray());
                    break;
                }
            }
        }
        return actualEntity;
    }

    private <T> HttpClientRequest createRequest(String httpMethodName) {
        HttpClient httpClient = builder.httpClient;
        URI uri = builder.uri;
        HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.valueOf(httpMethodName), uri.getPort(),
                uri.getHost(),
                uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));

        return httpClientRequest;
    }
}
