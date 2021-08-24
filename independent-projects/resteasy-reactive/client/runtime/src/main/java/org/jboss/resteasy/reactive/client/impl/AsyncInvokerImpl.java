package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.util.types.Types;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class AsyncInvokerImpl implements AsyncInvoker, CompletionStageRxInvoker {

    public static final Buffer EMPTY_BUFFER = Buffer.buffer(new byte[0]);

    final HttpClient httpClient;
    final URI uri;
    final RequestSpec requestSpec;
    final ConfigurationImpl configuration;
    final Map<String, Object> properties;
    final ClientImpl restClient;
    final HandlerChain handlerChain;
    final ThreadSetupAction requestContext;

    public AsyncInvokerImpl(ClientImpl restClient, HttpClient httpClient, URI uri, RequestSpec requestSpec,
            ConfigurationImpl configuration,
            Map<String, Object> properties, HandlerChain handlerChain,
            ThreadSetupAction requestContext) {
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.uri = uri;
        this.requestSpec = new RequestSpec(requestSpec);
        this.configuration = configuration;
        this.properties = new HashMap<>(properties);
        this.handlerChain = handlerChain;
        this.requestContext = requestContext;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public CompletableFuture<Response> get() {
        return method("GET");
    }

    @Override
    public <T> CompletableFuture<T> get(Class<T> responseType) {
        return method("GET", responseType);
    }

    @Override
    public <T> CompletableFuture<T> get(GenericType<T> responseType) {
        return method("GET", responseType);
    }

    @Override
    public <T> CompletableFuture<T> get(InvocationCallback<T> callback) {
        return method("GET", callback);
    }

    @Override
    public CompletableFuture<Response> put(Entity<?> entity) {
        return method("PUT", entity);
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, Class<T> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, GenericType<T> responseType) {
        return method("PUT", entity, responseType);
    }

    @Override
    public <T> CompletableFuture<T> put(Entity<?> entity, InvocationCallback<T> callback) {
        return method("PUT", entity, callback);
    }

    @Override
    public CompletableFuture<Response> post(Entity<?> entity) {
        return method("POST", entity);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, Class<T> responseType) {
        return method("POST", entity, responseType);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, GenericType<T> responseType) {
        return method("POST", entity, responseType);
    }

    @Override
    public <T> CompletableFuture<T> post(Entity<?> entity, InvocationCallback<T> callback) {
        return method("POST", entity, callback);
    }

    @Override
    public CompletableFuture<Response> delete() {
        return method("DELETE");
    }

    @Override
    public <T> CompletableFuture<T> delete(Class<T> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public <T> CompletableFuture<T> delete(GenericType<T> responseType) {
        return method("DELETE", responseType);
    }

    @Override
    public <T> CompletableFuture<T> delete(InvocationCallback<T> callback) {
        return method("DELETE", callback);
    }

    @Override
    public CompletableFuture<Response> head() {
        return method("HEAD");
    }

    @Override
    public Future<Response> head(InvocationCallback<Response> callback) {
        return method("HEAD", callback);
    }

    @Override
    public CompletableFuture<Response> options() {
        return method("OPTIONS");
    }

    @Override
    public <T> CompletableFuture<T> options(Class<T> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public <T> CompletableFuture<T> options(GenericType<T> responseType) {
        return method("OPTIONS", responseType);
    }

    @Override
    public <T> CompletableFuture<T> options(InvocationCallback<T> callback) {
        return method("OPTIONS", callback);
    }

    @Override
    public CompletableFuture<Response> trace() {
        return method("TRACE");
    }

    @Override
    public <T> CompletableFuture<T> trace(Class<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> CompletableFuture<T> trace(GenericType<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> CompletableFuture<T> trace(InvocationCallback<T> callback) {
        return method("TRACE", callback);
    }

    @Override
    public CompletableFuture<Response> method(String name) {
        return performRequestInternal(name, null, null);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Class<T> responseType) {
        return mapResponse(performRequestInternal(name, null, new GenericType<>(responseType)), responseType);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, GenericType<T> responseType) {
        return mapResponse(performRequestInternal(name, null, responseType), responseType.getRawType());
    }

    @Override
    public <T> CompletableFuture<T> method(String name, InvocationCallback<T> callback) {
        return method(name, null, callback);
    }

    @Override
    public CompletableFuture<Response> method(String name, Entity<?> entity) {
        return performRequestInternal(name, entity, null);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, Class<T> responseType) {
        CompletableFuture<Response> response = performRequestInternal(name, entity, new GenericType<>(responseType));
        return mapResponse(response, responseType);
    }

    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        //we default to String if no return type specified
        CompletableFuture<Response> response = performRequestInternal(name, entity,
                responseType == null ? new GenericType<>(String.class) : responseType);
        return mapResponse(response, responseType == null ? String.class : responseType.getRawType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CompletableFuture<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
        GenericType<Object> genericType = new GenericType<>(getInvocationCallbackType(callback));
        CompletableFuture<T> cf;
        if (genericType.getRawType().equals(Response.class)) {
            cf = (CompletableFuture<T>) performRequestInternal(name, entity, null);
        } else {
            cf = mapResponse(performRequestInternal(name, entity, genericType), genericType.getRawType());
        }

        cf.whenComplete(new BiConsumer<T, Throwable>() {
            @Override
            public void accept(T t, Throwable throwable) {
                if (throwable != null) {
                    callback.failed(throwable);
                } else {
                    callback.completed(t);
                }
            }
        });

        return cf;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> CompletableFuture<Response> performRequestInternal(String httpMethodName, Entity<?> entity,
            GenericType<?> responseType) {
        return (CompletableFuture) performRequestInternal(httpMethodName, entity, responseType, true).getResult();
    }

    RestClientRequestContext performRequestInternal(String httpMethodName, Entity<?> entity, GenericType<?> responseType,
            boolean registerBodyHandler) {
        RestClientRequestContext restClientRequestContext = new RestClientRequestContext(restClient, httpClient, httpMethodName,
                uri, requestSpec.configuration, requestSpec.headers,
                entity, responseType, registerBodyHandler, properties, handlerChain.createHandlerChain(configuration),
                handlerChain.createAbortHandlerChain(configuration),
                handlerChain.createAbortHandlerChainWithoutResponseFilters(), requestContext);
        restClientRequestContext.run();
        return restClientRequestContext;
    }

    private <T> Type getInvocationCallbackType(InvocationCallback<T> callback) {
        Class<?> knownFromBuildTime = restClient.getClientContext().getGenericTypeMapping()
                .forInvocationCallback(callback.getClass());
        if (knownFromBuildTime != null) {
            return knownFromBuildTime;
        }

        Type[] typeInfo = Types.getActualTypeArgumentsOfAnInterface(callback.getClass(), InvocationCallback.class);
        if (typeInfo.length == 1) {
            return typeInfo[0];
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> mapResponse(CompletableFuture<Response> res, Class<?> responseType) {
        if (responseType.equals(Response.class)) {
            return (CompletableFuture<T>) res;
        }
        return res.thenApply(new Function<Response, T>() {
            @Override
            public T apply(Response response) {
                return (T) response.getEntity();
            }
        });
    }
}
