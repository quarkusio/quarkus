package io.quarkus.rest.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.api.WebClientApplicationException;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestResponse;
import io.quarkus.rest.runtime.util.CaseInsensitiveMap;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
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
    private boolean responseTypeSpecified;
    private final QuarkusRestClient restClient;
    final Serialisers serialisers;
    final ClientRequestHeaders requestHeaders;
    final QuarkusRestConfiguration configuration;
    private final boolean registerBodyHandler;
    // will be used to check if we need to throw a WebApplicationException
    // see Javadoc of javax.ws.rs.client.Invocation or javax.ws.rs.client.SyncInvoker
    private final boolean checkSuccessfulFamily;
    private final CompletableFuture<QuarkusRestResponse> result;
    /**
     * Only initialised if we have request or response filters
     */
    private QuarkusRestClientRequestContext requestContext;
    /**
     * Only initialised once we get the response
     */
    private HttpClientResponse vertxClientResponse;
    // Changed by the request filter
    Map<String, Object> properties;

    public InvocationState(QuarkusRestClient restClient,
            HttpClient httpClient, String httpMethod, URI uri,
            QuarkusRestConfiguration configuration, ClientRequestHeaders requestHeaders, Serialisers serialisers,
            Entity<?> entity, GenericType<?> responseType, boolean registerBodyHandler, Map<String, Object> properties) {
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.requestHeaders = requestHeaders;
        this.configuration = configuration;
        this.serialisers = serialisers;
        this.entity = entity;
        if (responseType == null) {
            this.responseType = new GenericType<>(String.class);
            this.checkSuccessfulFamily = false;
            this.responseTypeSpecified = false;
        } else {
            this.responseType = responseType;
            boolean isJaxResponse = responseType.getRawType().equals(Response.class);
            this.checkSuccessfulFamily = !isJaxResponse;
            this.responseTypeSpecified = !isJaxResponse;
        }
        this.registerBodyHandler = registerBodyHandler;
        this.result = new CompletableFuture<>();
        // each invocation gets a new set of properties based on the JAX-RS invoker
        this.properties = new HashMap<>(properties);
        start();
    }

    private void start() {
        try {
            runRequestFilters();
            if (requestContext != null && requestContext.abortedWith != null) {
                // just run the response filters
                QuarkusRestClientResponseContext context = new QuarkusRestClientResponseContext(
                        requestContext.abortedWith.getStatus(), requestContext.abortedWith.getStatusInfo().getReasonPhrase(),
                        requestContext.abortedWith.getStringHeaders());
                ensureResponseAndRunFilters(context, requestContext.abortedWith);
            } else {
                HttpClientRequest httpClientRequest = createRequest();
                Buffer actualEntity = setRequestHeadersAndPrepareBody(httpClientRequest);
                httpClientRequest.handler(this);
                httpClientRequest.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable event) {
                        if (event instanceof IOException) {
                            result.completeExceptionally(new ProcessingException(event));
                        }
                        result.completeExceptionally(event);
                    }
                });
                if (actualEntity == QuarkusRestAsyncInvoker.EMPTY_BUFFER) {
                    httpClientRequest.end();
                } else {
                    httpClientRequest.end(actualEntity);
                }

            }
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }

    private void runRequestFilters() {
        List<ClientRequestFilter> filters = configuration.getRequestFilters();
        if (!filters.isEmpty()) {
            requestContext = new QuarkusRestClientRequestContext(this, restClient, configuration);

            for (ClientRequestFilter filter : filters) {
                try {
                    filter.filter(requestContext);
                } catch (Exception x) {
                    throw new ProcessingException(x);
                }
                if (requestContext.abortedWith != null) {
                    return;
                }
            }
        }
    }

    public <T> T readEntity(InputStream in,
            GenericType<T> responseType, MediaType mediaType,
            MultivaluedMap<String, Object> metadata)
            throws IOException {
        if (in == null)
            return null;
        List<MessageBodyReader<?>> readers = serialisers.findReaders(configuration, responseType.getRawType(),
                mediaType, RuntimeType.CLIENT);
        // FIXME
        Annotation[] annotations = null;
        for (MessageBodyReader<?> reader : readers) {
            if (reader.isReadable(responseType.getRawType(), responseType.getType(), annotations,
                    mediaType)) {
                return (T) Serialisers.invokeClientReader(annotations, responseType.getRawType(), responseType.getType(),
                        mediaType, properties, metadata, reader, in, getReaderInterceptors());
            }
        }

        // FIXME: exception?
        return null;
    }

    ReaderInterceptor[] getReaderInterceptors() {
        return configuration.getReaderInterceptors().toArray(Serialisers.NO_READER_INTERCEPTOR);
    }

    private QuarkusRestClientResponseContext initialiseResponse(HttpClientResponse vertxResponse) {
        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        for (String i : vertxResponse.headers().names()) {
            headers.addAll(i, vertxResponse.getHeader(i));
        }
        this.vertxClientResponse = vertxResponse;
        return new QuarkusRestClientResponseContext(vertxResponse.statusCode(), vertxResponse.statusMessage(), headers);
    }

    private <T> Buffer setRequestHeadersAndPrepareBody(HttpClientRequest httpClientRequest)
            throws IOException {
        MultivaluedMap<String, String> headerMap = requestHeaders.asMap();
        MultiMap vertxHttpHeaders = httpClientRequest.headers();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            vertxHttpHeaders.add(entry.getKey(), entry.getValue());
        }
        Buffer actualEntity = QuarkusRestAsyncInvoker.EMPTY_BUFFER;
        if (entity != null) {
            if (entity.getMediaType() != null) {
                vertxHttpHeaders.set(HttpHeaders.CONTENT_TYPE, entity.getMediaType().toString());
            }
            if (entity.getVariant() != null) {
                Variant v = entity.getVariant();
                vertxHttpHeaders.set(HttpHeaders.CONTENT_TYPE, v.getMediaType().toString());
                vertxHttpHeaders.set(HttpHeaders.CONTENT_LANGUAGE, v.getLanguageString());
                vertxHttpHeaders.set(HttpHeaders.CONTENT_ENCODING, v.getEncoding());
            }

            actualEntity = writeEntity(entity, headerMap,
                    configuration.getWriterInterceptors().toArray(Serialisers.NO_WRITER_INTERCEPTOR));
        }
        return actualEntity;
    }

    private Buffer writeEntity(Entity<?> entity, MultivaluedMap<String, String> headerMap, WriterInterceptor[] interceptors)
            throws IOException {
        Object entityObject = entity.getEntity();
        Class<?> entityClass;
        Type entityType;
        if (entityObject instanceof GenericEntity) {
            GenericEntity<?> genericEntity = (GenericEntity<?>) entityObject;
            entityClass = genericEntity.getRawType();
            entityType = genericEntity.getType();
            entityObject = genericEntity.getEntity();
        } else {
            entityType = entityClass = entityObject.getClass();
        }
        List<MessageBodyWriter<?>> writers = serialisers.findWriters(configuration, entityClass, entity.getMediaType(),
                RuntimeType.CLIENT);
        for (MessageBodyWriter<?> w : writers) {
            Buffer ret = Serialisers.invokeClientWriter(entity, entityObject, entityClass, entityType, headerMap, w,
                    interceptors,
                    properties);
            if (ret != null) {
                return ret;
            }
        }
        // FIXME: exception?
        return null;
    }

    private <T> HttpClientRequest createRequest() {
        HttpClient httpClient = this.httpClient;
        URI uri = this.uri;
        HttpClientRequest httpClientRequest = httpClient.request(HttpMethod.valueOf(httpMethod), uri.getPort(),
                uri.getHost(),
                uri.getPath() + (uri.getQuery() == null ? "" : "?" + uri.getQuery()));

        return httpClientRequest;
    }

    private void ensureResponseAndRunFilters(QuarkusRestClientResponseContext responseContext, Response abortedWith)
            throws IOException {
        if (checkSuccessfulFamily && (responseContext.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)) {
            throw new WebClientApplicationException("Server response status was: " + responseContext.getStatus());
        }
        // the spec doesn't really say this, but the TCK checks that the abortWith entity ends up read
        // so we have to write it, but without filters/interceptors
        if (abortedWith != null) {
            setExistingEntity(abortedWith, responseContext);
        }

        List<ClientResponseFilter> filters = configuration.getResponseFilters();
        if (!filters.isEmpty()) {
            if (requestContext == null)
                requestContext = new QuarkusRestClientRequestContext(this, restClient, configuration);
            // FIXME: pretty sure we'll have to mark it as immutable in this phase, but the spec is not verbose about this
            // the server does it.
            for (ClientResponseFilter filter : filters) {
                try {
                    filter.filter(requestContext, responseContext);
                } catch (Exception x) {
                    throw new ProcessingException(x);
                }
            }
        }
        QuarkusRestClientResponseBuilder builder = new QuarkusRestClientResponseBuilder();
        builder.status(responseContext.getStatus(), responseContext.getReasonPhrase());
        builder.setAllHeaders(responseContext.getHeaders());
        builder.invocationState(this);
        if (responseTypeSpecified) { // this case means that a specific response type was requested
            Object entity = readEntity(responseContext.getEntityStream(),
                    responseType,
                    responseContext.getMediaType(),
                    // FIXME: we have strings, it wants objects, perhaps there's
                    // an Object->String conversion too many
                    (MultivaluedMap) responseContext.getHeaders());
            if (entity != null) {
                builder.entity(entity);
            }
        } else {
            // in this case no specific response type was requested so we just prepare the stream
            // the users of the response are meant to use readEntity
            builder.entityStream(responseContext.getEntityStream());
        }
        result.complete(builder.build());
    }

    private void setExistingEntity(Response abortedWith, QuarkusRestClientResponseContext responseContext) throws IOException {
        Object value = abortedWith.getEntity();
        if (value == null) {
            responseContext.setEntityStream(null);
            return;
        }
        Entity entity;
        if (value instanceof Entity) {
            entity = (Entity) value;
        } else {
            MediaType mediaType = abortedWith.getMediaType();
            if (mediaType == null) {
                // FIXME: surely this is wrong, perhaps we can use the expected response type?
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            }
            entity = Entity.entity(value, mediaType);
        }
        // FIXME: pass headers?
        Buffer buffer = writeEntity(entity, (MultivaluedMap) Serialisers.EMPTY_MULTI_MAP, Serialisers.NO_WRITER_INTERCEPTOR);
        responseContext.setEntityStream(new ByteArrayInputStream(buffer.getBytes()));
    }

    @Override
    public void handle(HttpClientResponse clientResponse) {
        try {
            QuarkusRestClientResponseContext context = initialiseResponse(clientResponse);
            if (!registerBodyHandler) {
                ensureResponseAndRunFilters(context, null);
            } else {
                clientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        try {
                            if (buffer.length() > 0)
                                context.setEntityStream(new ByteArrayInputStream(buffer.getBytes()));
                            else
                                context.setEntityStream(null);
                            ensureResponseAndRunFilters(context, null);
                        } catch (Throwable t) {
                            result.completeExceptionally(t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            result.completeExceptionally(t);
            return;
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
