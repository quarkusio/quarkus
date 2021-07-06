package org.jboss.resteasy.reactive.client.impl;

import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.READ_TIMEOUT;

import io.vertx.core.Context;
import io.vertx.core.http.HttpClient;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class InvocationBuilderImpl implements Invocation.Builder {

    private static final long DEFAULT_READ_TIMEOUT = 30_000L;

    final URI uri;
    final HttpClient httpClient;
    final WebTargetImpl target;
    final RequestSpec requestSpec;
    final Map<String, Object> properties = new HashMap<>();
    final ConfigurationImpl configuration;
    final ClientImpl restClient;
    final HandlerChain handlerChain;
    final ThreadSetupAction requestContext;
    final long readTimeoutMs;

    public InvocationBuilderImpl(URI uri, ClientImpl restClient, HttpClient httpClient,
            WebTargetImpl target,
            ConfigurationImpl configuration, HandlerChain handlerChain, ThreadSetupAction requestContext) {
        this.uri = uri;
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.target = target;
        this.requestSpec = new RequestSpec(configuration);
        this.configuration = configuration;
        this.handlerChain = handlerChain;
        this.requestContext = requestContext;
        Object readTimeoutMs = configuration.getProperty(READ_TIMEOUT);
        if (readTimeoutMs == null) {
            this.readTimeoutMs = DEFAULT_READ_TIMEOUT;
        } else {
            this.readTimeoutMs = (long) readTimeoutMs;
        }
    }

    @Override
    public Invocation build(String method) {
        return new InvocationImpl(method, async(), null);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return new InvocationImpl(method, async(), entity);
    }

    @Override
    public Invocation buildGet() {
        return build("GET");
    }

    @Override
    public Invocation buildDelete() {
        return build("DELETE");
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return build("POST", entity);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return build("PUT", entity);
    }

    @Override
    public AsyncInvokerImpl async() {
        return new AsyncInvokerImpl(restClient, httpClient, uri, requestSpec, configuration,
                properties, handlerChain, requestContext);
    }

    @Override
    public Invocation.Builder accept(String... mediaTypes) {
        requestSpec.headers.accept(mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder accept(MediaType... mediaTypes) {
        requestSpec.headers.accept(mediaTypes);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(Locale... locales) {
        requestSpec.headers.acceptLanguage(locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(String... locales) {
        requestSpec.headers.acceptLanguage(locales);
        return this;
    }

    @Override
    public Invocation.Builder acceptEncoding(String... encodings) {
        requestSpec.headers.acceptEncoding(encodings);
        return this;
    }

    @Override
    public Invocation.Builder cookie(Cookie cookie) {
        requestSpec.headers.cookie(cookie);
        return this;
    }

    @Override
    public Invocation.Builder cookie(String name, String value) {
        requestSpec.headers.cookie(new Cookie(name, value));
        return this;
    }

    @Override
    public Invocation.Builder cacheControl(CacheControl cacheControl) {
        requestSpec.headers.cacheControl(cacheControl);
        return this;
    }

    @Override
    public Invocation.Builder header(String name, Object value) {
        requestSpec.headers.header(name, value);
        return this;
    }

    @Override
    public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
        requestSpec.headers.setHeaders(headers);
        return this;
    }

    @Override
    public Invocation.Builder property(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    @Override
    public CompletionStageRxInvoker rx() {
        return new AsyncInvokerImpl(restClient, httpClient, uri, requestSpec, configuration,
                properties, handlerChain, requestContext);
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        if (clazz == MultiInvoker.class) {
            return (T) new MultiInvoker(this);
        } else if (clazz == UniInvoker.class) {
            return (T) new UniInvoker(this);
        }
        RxInvokerProvider<?> invokerProvider = requestSpec.configuration.getRxInvokerProvider(clazz);
        if (invokerProvider != null) {
            // FIXME: should pass the Quarkus executor here, but MP-CP or not?
            return (T) invokerProvider.getRxInvoker(this, null);
        }
        // TCK says we could throw IllegalStateException, or not, it doesn't discriminate, and the spec doesn't say
        return null;
    }

    @Override
    public Response get() {
        return unwrap(async().get());
    }

    private <T> T unwrap(CompletableFuture<T> c) {
        if (Context.isOnEventLoopThread()) {
            throw new BlockingNotAllowedException("Blocking REST client call made from the event loop. " +
                    "If the code is executed from a RESTEasy Reactive resource, either annotate the resource method " +
                    "with `@Blocking` or use non-blocking client calls.");
        }
        try {
            return c.get(readTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            throw new ProcessingException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingException) {
                throw (ProcessingException) e.getCause();
            }
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new ProcessingException(e.getCause().getMessage(), e.getCause());
        }
    }

    @Override
    public <T> T get(Class<T> responseType) {
        return unwrap(async().get(responseType));
    }

    @Override
    public <T> T get(GenericType<T> responseType) {
        return unwrap(async().get(responseType));
    }

    @Override
    public Response put(Entity<?> entity) {
        return unwrap(async().put(entity));
    }

    @Override
    public <T> T put(Entity<?> entity, Class<T> responseType) {
        return unwrap(async().put(entity, responseType));
    }

    @Override
    public <T> T put(Entity<?> entity, GenericType<T> responseType) {
        return unwrap(async().put(entity, responseType));
    }

    @Override
    public Response post(Entity<?> entity) {
        return unwrap(async().post(entity));
    }

    @Override
    public <T> T post(Entity<?> entity, Class<T> responseType) {
        return unwrap(async().post(entity, responseType));
    }

    @Override
    public <T> T post(Entity<?> entity, GenericType<T> responseType) {
        return unwrap(async().post(entity, responseType));
    }

    @Override
    public Response delete() {
        return unwrap(async().delete());
    }

    @Override
    public <T> T delete(Class<T> responseType) {
        return unwrap(async().delete(responseType));
    }

    @Override
    public <T> T delete(GenericType<T> responseType) {
        return unwrap(async().delete(responseType));
    }

    @Override
    public Response head() {
        return unwrap(async().head());
    }

    @Override
    public Response options() {
        return unwrap(async().options());
    }

    @Override
    public <T> T options(Class<T> responseType) {
        return unwrap(async().options(responseType));
    }

    @Override
    public <T> T options(GenericType<T> responseType) {
        return unwrap(async().options(responseType));
    }

    @Override
    public Response trace() {
        return unwrap(async().trace());
    }

    @Override
    public <T> T trace(Class<T> responseType) {
        return unwrap(async().trace(responseType));
    }

    @Override
    public <T> T trace(GenericType<T> responseType) {
        return unwrap(async().trace(responseType));
    }

    @Override
    public Response method(String name) {
        return unwrap(async().method(name));
    }

    @Override
    public <T> T method(String name, Class<T> responseType) {
        return unwrap(async().method(name, responseType));
    }

    @Override
    public <T> T method(String name, GenericType<T> responseType) {
        return unwrap(async().method(name, responseType));
    }

    @Override
    public Response method(String name, Entity<?> entity) {
        return unwrap(async().method(name, entity));
    }

    @Override
    public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
        return unwrap(async().method(name, entity, responseType));
    }

    @Override
    public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
        return unwrap(async().method(name, entity, responseType));
    }

    public WebTargetImpl getTarget() {
        return target;
    }

    public void setChunked(boolean chunked) {
        this.requestSpec.chunked = chunked;
    }

    public boolean getChunked() {
        return requestSpec.chunked;
    }

    public ClientRequestHeaders getHeaders() {
        return requestSpec.headers;
    }

}
