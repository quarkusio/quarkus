package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.web.multipart.MultipartForm;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.AbstractResteasyReactiveContext;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

/**
 * This is a stateful invocation, you can't invoke it twice.
 */
public class RestClientRequestContext extends AbstractResteasyReactiveContext<RestClientRequestContext, ClientRestHandler> {

    private final HttpClient httpClient;
    // Changeable by the request filter
    String httpMethod;
    // Changeable by the request filter
    URI uri;
    // Changeable by the request filter
    Entity<?> entity;
    GenericType<?> responseType;
    private boolean responseTypeSpecified;
    private final ClientImpl restClient;
    final ClientRequestHeaders requestHeaders;
    final ConfigurationImpl configuration;
    private final boolean registerBodyHandler;
    // will be used to check if we need to throw a WebApplicationException
    // see Javadoc of javax.ws.rs.client.Invocation or javax.ws.rs.client.SyncInvoker
    private final boolean checkSuccessfulFamily;
    private final CompletableFuture<ResponseImpl> result;
    private final ClientRestHandler[] abortHandlerChainWithoutResponseFilters;
    /**
     * Only initialised if we have request or response filters
     */
    /**
     * Only initialised once we get the response
     */
    private HttpClientResponse vertxClientResponse;
    // Changed by the request filter
    Map<String, Object> properties;
    private HttpClientRequest httpClientRequest;

    private int responseStatus;
    private String responseReasonPhrase;
    private MultivaluedMap<String, String> responseHeaders;
    private ClientRequestContextImpl clientRequestContext;
    private ClientResponseContextImpl clientResponseContext;
    private InputStream responseEntityStream;
    private Response abortedWith;

    public RestClientRequestContext(ClientImpl restClient,
            HttpClient httpClient, String httpMethod, URI uri,
            ConfigurationImpl configuration, ClientRequestHeaders requestHeaders,
            Entity<?> entity, GenericType<?> responseType, boolean registerBodyHandler, Map<String, Object> properties,
            ClientRestHandler[] handlerChain,
            ClientRestHandler[] abortHandlerChain,
            ClientRestHandler[] abortHandlerChainWithoutResponseFilters,
            ThreadSetupAction requestContext) {
        super(handlerChain, abortHandlerChain, requestContext);
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.requestHeaders = requestHeaders;
        this.configuration = configuration;
        this.entity = entity;
        this.abortHandlerChainWithoutResponseFilters = abortHandlerChainWithoutResponseFilters;
        if (responseType == null) {
            this.responseType = new GenericType<>(String.class);
            this.checkSuccessfulFamily = false;
            this.responseTypeSpecified = false;
        } else {
            this.responseType = responseType;
            if (responseType.getRawType().equals(Response.class)) {
                this.checkSuccessfulFamily = false;
                this.responseTypeSpecified = false;
            } else if (responseType.getRawType().equals(RestResponse.class)) {
                if (responseType.getType() instanceof ParameterizedType) {
                    ParameterizedType type = (ParameterizedType) responseType.getType();
                    if (type.getActualTypeArguments().length == 1) {
                        Type restResponseType = type.getActualTypeArguments()[0];
                        this.responseType = new GenericType<>(restResponseType);
                    }
                }
                this.checkSuccessfulFamily = false;
                this.responseTypeSpecified = true;
            } else {
                this.checkSuccessfulFamily = true;
                this.responseTypeSpecified = true;
            }
        }
        this.registerBodyHandler = registerBodyHandler;
        this.result = new CompletableFuture<>();
        // each invocation gets a new set of properties based on the JAX-RS invoker
        this.properties = new HashMap<>(properties);
    }

    public void abort() {
        setAbortHandlerChainStarted(true);
        restart(abortHandlerChain);
    }

    @Override
    protected Throwable unwrapException(Throwable t) {
        var res = super.unwrapException(t);
        if (res instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) res;
            return new ClientWebApplicationException(webApplicationException.getMessage(), webApplicationException,
                    webApplicationException.getResponse());
        }
        return res;
    }

    public <T> T readEntity(InputStream in,
            GenericType<T> responseType, MediaType mediaType,
            MultivaluedMap<String, Object> metadata)
            throws IOException {
        if (in == null)
            return null;
        return (T) ClientSerialisers.invokeClientReader(null, responseType.getRawType(), responseType.getType(),
                mediaType, properties, metadata, restClient.getClientContext().getSerialisers(), in, getReaderInterceptors(),
                configuration);
    }

    ReaderInterceptor[] getReaderInterceptors() {
        return configuration.getReaderInterceptors().toArray(Serialisers.NO_READER_INTERCEPTOR);
    }

    public void initialiseResponse(HttpClientResponse vertxResponse) {
        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        MultiMap vertxHeaders = vertxResponse.headers();
        for (String i : vertxHeaders.names()) {
            headers.addAll(i, vertxHeaders.getAll(i));
        }
        this.vertxClientResponse = vertxResponse;
        responseStatus = vertxResponse.statusCode();
        responseReasonPhrase = vertxResponse.statusMessage();
        responseHeaders = headers;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ClientRequestContextImpl getClientRequestContext() {
        return clientRequestContext;
    }

    public ClientResponseContextImpl getOrCreateClientResponseContext() {
        if (clientResponseContext == null) {
            clientResponseContext = new ClientResponseContextImpl(this);
        }
        return clientResponseContext;
    }

    public ClientRequestContextImpl getOrCreateClientRequestContext() {
        if (clientRequestContext == null) {
            clientRequestContext = new ClientRequestContextImpl(this, this.getRestClient(), this.getConfiguration());
        }
        return clientRequestContext;
    }

    public Buffer writeEntity(Entity<?> entity, MultivaluedMap<String, String> headerMap, WriterInterceptor[] interceptors)
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
        List<MessageBodyWriter<?>> writers = restClient.getClientContext().getSerialisers().findWriters(configuration,
                entityClass, entity.getMediaType(),
                RuntimeType.CLIENT);
        for (MessageBodyWriter<?> w : writers) {
            Buffer ret = ClientSerialisers.invokeClientWriter(entity, entityObject, entityClass, entityType, headerMap, w,
                    interceptors, properties, restClient.getClientContext().getSerialisers(), configuration);
            if (ret != null) {
                return ret;
            }
        }
        // FIXME: exception?
        return null;
    }

    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {
        this.entity = Entity.entity(entity, mediaType, annotations);
    }

    public CompletableFuture<ResponseImpl> getResult() {
        return result;
    }

    public HttpClientResponse getVertxClientResponse() {
        return vertxClientResponse;
    }

    @Override
    protected Executor getEventLoop() {
        if (httpClientRequest == null) {
            // make sure we execute the client callbacks on the same context as the current thread
            Context context = restClient.getVertx().getOrCreateContext();
            return new Executor() {
                @Override
                public void execute(Runnable command) {
                    context.runOnContext(v -> command.run());
                }
            };
        } else {
            return new Executor() {
                @Override
                public void execute(Runnable command) {
                    command.run();
                }
            };
        }
    }

    public HttpClientRequest getHttpClientRequest() {
        return httpClientRequest;
    }

    public RestClientRequestContext setHttpClientRequest(HttpClientRequest httpClientRequest) {
        this.httpClientRequest = httpClientRequest;
        return this;
    }

    @Override
    protected void handleRequestScopeActivation() {

    }

    @Override
    protected void restarted(boolean keepTarget) {

    }

    @Override
    public void close() {
        super.close();
        if (!result.isDone()) {
            try {
                ClientRestHandler[] handlers = this.handlers;
                String[] handlerClassNames = new String[handlers.length];
                for (int i = 0; i < handlers.length; i++) {
                    handlerClassNames[i] = handlers[i].getClass().getName();
                }
                log.error("Client was closed, however the result was not completed. Handlers array is: "
                        + Arrays.toString(handlerClassNames)
                        + ". Last executed handler is: " + handlers[position - 1].getClass().getName());
            } catch (Exception ignored) {
                // we don't want some mistake in the code above to compromise the ability to return a result
            }
            result.completeExceptionally(new IllegalStateException("Client request did not complete"));
        }
    }

    @Override
    protected void handleUnrecoverableError(Throwable throwable) {
        result.completeExceptionally(throwable);
    }

    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    public ClientImpl getRestClient() {
        return restClient;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public RestClientRequestContext setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public String getResponseReasonPhrase() {
        return responseReasonPhrase;
    }

    public RestClientRequestContext setResponseReasonPhrase(String responseReasonPhrase) {
        this.responseReasonPhrase = responseReasonPhrase;
        return this;
    }

    public MultivaluedMap<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public RestClientRequestContext setResponseHeaders(MultivaluedMap<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }

    public boolean isCheckSuccessfulFamily() {
        return checkSuccessfulFamily;
    }

    public boolean isResponseTypeSpecified() {
        return responseTypeSpecified;
    }

    public RestClientRequestContext setResponseTypeSpecified(boolean responseTypeSpecified) {
        this.responseTypeSpecified = responseTypeSpecified;
        return this;
    }

    public GenericType<?> getResponseType() {
        return responseType;
    }

    public RestClientRequestContext setResponseType(GenericType<?> responseType) {
        this.responseType = responseType;
        return this;
    }

    public ClientRequestHeaders getRequestHeaders() {
        return requestHeaders;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public RestClientRequestContext setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public RestClientRequestContext setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Entity<?> getEntity() {
        return entity;
    }

    public RestClientRequestContext setEntity(Entity<?> entity) {
        this.entity = entity;
        return this;
    }

    public boolean isRegisterBodyHandler() {
        return registerBodyHandler;
    }

    public InputStream getResponseEntityStream() {
        return responseEntityStream;
    }

    public RestClientRequestContext setResponseEntityStream(InputStream responseEntityStream) {
        this.responseEntityStream = responseEntityStream;
        return this;
    }

    public boolean isAborted() {
        return getAbortedWith() != null;
    }

    public Response getAbortedWith() {
        return abortedWith;
    }

    public RestClientRequestContext setAbortedWith(Response abortedWith) {
        this.abortedWith = abortedWith;
        return this;
    }

    public boolean isMultipart() {
        return entity != null && entity.getEntity() instanceof MultipartForm;
    }

    public Map<String, Object> getClientFilterProperties() {
        return properties;
    }

    public ClientRestHandler[] getAbortHandlerChainWithoutResponseFilters() {
        return abortHandlerChainWithoutResponseFilters;
    }
}
