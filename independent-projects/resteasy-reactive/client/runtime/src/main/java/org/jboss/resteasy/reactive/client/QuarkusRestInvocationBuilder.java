package org.jboss.resteasy.reactive.client;

import io.vertx.core.http.HttpClient;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.jaxrs.QuarkusRestConfiguration;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class QuarkusRestInvocationBuilder implements Invocation.Builder {

    final URI uri;
    final HttpClient httpClient;
    final QuarkusRestWebTarget target;
    final RequestSpec requestSpec;
    final Map<String, Object> properties = new HashMap<>();
    final QuarkusRestClient restClient;
    final ClientRestHandler[] handlerChain;
    final ClientRestHandler[] abortHandlerChain;
    final ThreadSetupAction requestContext;

    public QuarkusRestInvocationBuilder(URI uri, QuarkusRestClient restClient, HttpClient httpClient,
            QuarkusRestWebTarget target,
            QuarkusRestConfiguration configuration, ClientRestHandler[] handlerChain,
            ClientRestHandler[] abortHandlerChain, ThreadSetupAction requestContext) {
        this.uri = uri;
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.target = target;
        this.requestSpec = new RequestSpec(configuration);
        this.handlerChain = handlerChain;
        this.abortHandlerChain = abortHandlerChain;
        this.requestContext = requestContext;
    }

    @Override
    public Invocation build(String method) {
        return new QuarkusRestInvocation(method, async(), null);
    }

    @Override
    public Invocation build(String method, Entity<?> entity) {
        return new QuarkusRestInvocation(method, async(), entity);
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
    public QuarkusRestAsyncInvoker async() {
        return new QuarkusRestAsyncInvoker(restClient, httpClient, uri, requestSpec,
                properties, handlerChain, abortHandlerChain, requestContext);
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
        return new QuarkusRestAsyncInvoker(restClient, httpClient, uri, requestSpec,
                properties, handlerChain, abortHandlerChain, requestContext);
    }

    @Override
    public <T extends RxInvoker> T rx(Class<T> clazz) {
        if (clazz == QuarkusRestMultiInvoker.class) {
            return (T) new QuarkusRestMultiInvoker(target);
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
        try {
            return c.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingException) {
                throw (ProcessingException) e.getCause();
            }
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
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

    public QuarkusRestWebTarget getTarget() {
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
