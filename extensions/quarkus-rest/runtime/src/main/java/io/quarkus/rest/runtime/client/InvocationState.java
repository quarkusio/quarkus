package io.quarkus.rest.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponseBuilder;
import io.quarkus.rest.runtime.util.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * This is a stateful invocation, you can't invoke it twice.
 */
public class InvocationState implements Handler<HttpClientResponse> {

    private final HttpClient httpClient;
    // Changeable by the request filter
    String httpMethod;
    // Changeable by the request filter
    URI uri;
    // Changeable by the request filter
    Entity<?> entity;
    GenericType<?> responseType;
    private final QuarkusRestClient restClient;
    private final Serialisers serialisers;
    final ClientRequestHeaders requestHeaders;
    private final boolean registerBodyHandler;
    private final CompletableFuture<QuarkusRestResponse> result;
    /**
     * Only initialised if we have request or response filters
     */
    private QuarkusRestClientRequestContext requestContext;
    /**
     * Only initialised once we get the response
     */
    private HttpClientResponse vertxClientResponse;

    public InvocationState(QuarkusRestClient restClient,
            HttpClient httpClient, String httpMethod, URI uri,
            ClientRequestHeaders requestHeaders, Serialisers serialisers,
            Entity<?> entity, GenericType<?> responseType, boolean registerBodyHandler) {
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.requestHeaders = requestHeaders;
        this.serialisers = serialisers;
        this.entity = entity;
        this.responseType = responseType != null ? responseType : new GenericType<>(String.class);
        this.registerBodyHandler = registerBodyHandler;
        this.result = new CompletableFuture<>();
        start();
    }

    private void start() {
        try {
            runRequestFilters();
            if (requestContext != null && requestContext.abortedWith != null) {
                // just run the response filters
                forwardResponse(requestContext.abortedWith);
            } else {
                HttpClientRequest httpClientRequest = createRequest();
                Buffer actualEntity = setRequestHeadersAndPrepareBody(httpClientRequest);
                httpClientRequest.handler(this);
                httpClientRequest.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        result.completeExceptionally(event);
                    }
                });
                httpClientRequest.end(actualEntity);
            }
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }

    private void forwardResponse(Response response) {
        result.complete(runResponseFilters((QuarkusRestResponse) response));
    }

    private void runRequestFilters() {
        QuarkusRestConfiguration configuration = restClient.getConfiguration();
        List<ClientRequestFilter> filters = configuration.getRequestFilters();
        if (!filters.isEmpty()) {
            requestContext = new QuarkusRestClientRequestContext(this, restClient);

            for (ClientRequestFilter filter : filters) {
                try {
                    filter.filter(requestContext);
                } catch (IOException e) {
                    // FIXME: What do we do here?
                    e.printStackTrace();
                }
                if (requestContext.abortedWith != null) {
                    return;
                }
            }
        }
    }

    public <T> T readEntity(Buffer buffer,
            GenericType<T> responseType, MediaType mediaType, MultivaluedMap<String, Object> metadata)
            throws IOException {
        List<MessageBodyReader<?>> readers = serialisers.findReaders(responseType.getRawType(),
                mediaType);
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(responseType.getRawType(), responseType.getType(), null,
                    mediaType)) {
                ByteArrayInputStream in = new ByteArrayInputStream(buffer.getBytes());
                return (T) ((MessageBodyReader) reader).readFrom(responseType.getRawType(), responseType.getType(),
                        null, mediaType, metadata, in);
            }
        }

        return (T) buffer.toString(StandardCharsets.UTF_8);
    }

    private MediaType initialiseResponse(HttpClientResponse vertxResponse, QuarkusRestResponseBuilder response) {
        MediaType mediaType = MediaType.WILDCARD_TYPE;
        for (String i : vertxResponse.headers().names()) {
            response.header(i, vertxResponse.getHeader(i));

        }
        String mediaTypeHeader = vertxResponse.getHeader(HttpHeaderNames.CONTENT_TYPE);
        if (mediaTypeHeader != null) {
            mediaType = MediaType.valueOf(mediaTypeHeader);
        }
        response.status(vertxResponse.statusCode());
        this.vertxClientResponse = vertxResponse;
        return mediaType;
    }

    private <T> Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest)
            throws IOException {
        MultivaluedMap<String, String> headerMap = requestHeaders.asMap();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            httpClientRequest.headers().add(entry.getKey(), entry.getValue());
        }
        if (entity != null && entity.getMediaType() != null) {
            httpClientRequest.headers().set(HttpHeaders.CONTENT_TYPE, entity.getMediaType().toString());
        }
        Buffer actualEntity = QuarkusRestAsyncInvoker.EMPTY_BUFFER;
        if (entity != null) {

            Class<?> entityType = entity.getEntity().getClass();
            List<MessageBodyWriter<?>> writers = serialisers.findWriters(entityType, entity.getMediaType());
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

    private <T> HttpClientRequest createRequest() {
        HttpClient httpClient = this.httpClient;
        URI uri = this.uri;
        HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.valueOf(httpMethod), uri.getPort(),
                uri.getHost(),
                uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));

        return httpClientRequest;
    }

    private QuarkusRestResponse runResponseFilters(QuarkusRestResponse response) {
        List<ClientResponseFilter> filters = restClient.getConfiguration().getResponseFilters();
        if (!filters.isEmpty()) {
            if (requestContext == null)
                requestContext = new QuarkusRestClientRequestContext(this, restClient);
            // FIXME: pretty sure we'll have to mark it as immutable in this phase, but the spec is not verbose about this
            // the server does it.
            ClientResponseContext responseContext = new QuarkusRestClientResponseContext(response);

            for (ClientResponseFilter filter : filters) {
                try {
                    filter.filter(requestContext, responseContext);
                } catch (IOException e) {
                    // FIXME: What do we do here?
                    e.printStackTrace();
                }
            }
        }
        return response;
    }

    @Override
    public void handle(HttpClientResponse clientResponse) {
        QuarkusRestResponseBuilder response;
        MediaType mediaType;
        try {
            response = new QuarkusRestResponseBuilder();
            mediaType = initialiseResponse(clientResponse, response);
        } catch (Throwable t) {
            result.completeExceptionally(t);
            return;
        }
        if (!registerBodyHandler) {
            result.complete(response.build());
        } else {
            clientResponse.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                    try {
                        Object entity = readEntity(buffer, InvocationState.this.responseType, mediaType,
                                response.getMetadata());
                        response.entity(entity);
                        forwardResponse(response.build());
                    } catch (Throwable t) {
                        result.completeExceptionally(t);
                    }
                }
            });
        }
    }

    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        this.entity = Entity.entity(entity, mediaType, annotations);
    }

    public CompletableFuture<QuarkusRestResponse> getResult() {
        return result;
    }

    public HttpClientResponse getVertxClientResponse() {
        return vertxClientResponse;
    }
}
