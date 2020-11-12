package io.quarkus.rest.server.runtime;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.enterprise.event.Event;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.common.http.ServerHttpRequest;
import org.jboss.resteasy.reactive.common.http.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRequestContext extends ResteasyReactiveRequestContext implements ServerHttpRequest, ServerHttpResponse {

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            QuarkusRequestContext::createEvent);
    final RoutingContext context;
    final HttpServerRequest request;
    final HttpServerResponse response;

    public QuarkusRequestContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers, RoutingContext context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
        super(deployment, providers, requestContext, handlerChain, abortHandlerChain);
        this.context = context;
        this.request = context.request();
        this.response = context.response();
    }

    public RoutingContext getContext() {
        return context;
    }

    @Override
    public ServerHttpRequest serverRequest() {
        return this;
    }

    @Override
    public ServerHttpResponse serverResponse() {
        return this;
    }

    protected void handleRequestScopeActivation() {
        super.handleRequestScopeActivation();
        QuarkusHttpUser user = (QuarkusHttpUser) context.user();
        if (user != null) {
            fireSecurityIdentity(user.getSecurityIdentity());
        }
    }

    static void fireSecurityIdentity(SecurityIdentity identity) {
        SECURITY_IDENTITY_EVENT.get().fire(identity);
    }

    static void clear() {
        SECURITY_IDENTITY_EVENT.clear();
    }

    private static Event<SecurityIdentity> createEvent() {
        return Arc.container().beanManager().getEvent().select(SecurityIdentity.class);
    }

    protected SecurityContext createSecurityContext() {
        return new QuarkusRestSecurityContext(context);
    }

    @Override
    protected EventLoop getEventLoop() {
        return ((ConnectionBase) context.request()).channel().eventLoop();
    }

    @Override
    public Vertx vertx() {
        return VertxCoreRecorder.getVertx().get();
    }

    @Override
    public String getRequestHeader(CharSequence name) {
        return request.headers().get(name);
    }

    @Override
    public Iterable<Map.Entry<String, String>> getAllRequestHeaders() {
        return request.headers();
    }

    @Override
    public List<String> getAllRequestHeaders(String name) {
        return request.headers().getAll(name);
    }

    @Override
    public boolean containsRequestHeader(CharSequence accept) {
        return request.headers().contains(accept);
    }

    @Override
    public String getRequestPath() {
        return request.path();
    }

    @Override
    public String getRequestMethod() {
        return request.rawMethod();
    }

    @Override
    public String getRequestNormalisedPath() {
        return context.normalisedPath();
    }

    @Override
    public String getRequestAbsoluteUri() {
        return request.absoluteURI();
    }

    @Override
    public String getRequestScheme() {
        return request.scheme();
    }

    @Override
    public String getRequestHost() {
        return request.host();
    }

    @Override
    public void closeConnection() {
        request.connection().close();
    }

    @Override
    public String getFormAttribute(String name) {
        return request.getFormAttribute(name);
    }

    @Override
    public List<String> getAllFormAttributes(String name) {
        return request.formAttributes().getAll(name);
    }

    @Override
    public String getQueryParam(String name) {
        return context.queryParams().get(name);
    }

    @Override
    public List<String> getAllQueryParams(String name) {
        return context.queryParam(name);
    }

    @Override
    public String query() {
        return request.query();
    }

    @Override
    public Collection<String> queryParamNames() {
        return context.queryParams().names();
    }

    @Override
    public boolean isRequestEnded() {
        return request.isEnded();
    }

    @Override
    public void setExpectMultipart(boolean expectMultipart) {
        request.setExpectMultipart(expectMultipart);
    }

    @Override
    public InputStream createInputStream(ByteBuf existingData) {
        return new VertxInputStream(context, 10000, existingData);
    }

    @Override
    public ServerHttpResponse pauseRequestInput() {
        request.pause();
        return this;
    }

    @Override
    public ServerHttpResponse resumeRequestInput() {
        request.resume();
        return this;
    }

    @Override
    public ServerHttpResponse setReadListener(ReadCallback callback) {
        request.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                callback.data(event);
            }
        });
        request.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                callback.done();
            }
        });
        return this;
    }

    @Override
    public ServerHttpResponse setStatusCode(int code) {
        response.setStatusCode(code);
        return this;
    }

    @Override
    public ServerHttpResponse end() {
        response.end();
        return this;
    }

    @Override
    public boolean headWritten() {
        return response.headWritten();
    }

    @Override
    public ServerHttpResponse end(byte[] data) {
        response.end(Buffer.buffer(data));
        return this;
    }

    @Override
    public ServerHttpResponse end(String data) {
        response.end(data);
        return this;
    }

    @Override
    public ServerHttpResponse addResponseHeader(CharSequence name, CharSequence value) {
        response.headers().add(name, value);
        return this;
    }

    @Override
    public ServerHttpResponse setResponseHeader(CharSequence name, CharSequence value) {
        response.headers().set(name, value);
        return this;
    }

    @Override
    public ServerHttpResponse setResponseHeader(CharSequence name, Iterable<CharSequence> values) {
        response.headers().set(name, values);
        return this;
    }

    @Override
    public Iterable<Map.Entry<String, String>> getAllResponseHeaders() {
        return response.headers();
    }

    @Override
    public boolean closed() {
        return response.closed();
    }

    @Override
    public ServerHttpResponse setChunked(boolean chunked) {
        response.setChunked(chunked);
        return this;
    }

    @Override
    public ServerHttpResponse write(byte[] data, Consumer<Throwable> asyncResultHandler) {
        response.write(Buffer.buffer(data), new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.failed()) {
                    asyncResultHandler.accept(event.cause());
                } else {
                    asyncResultHandler.accept(null);
                }
            }
        });
        return this;
    }

    @Override
    public CompletionStage<Void> write(byte[] data) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        response.write(Buffer.buffer(data), new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.failed()) {
                    ret.completeExceptionally(event.cause());
                } else {
                    ret.complete(null);
                }
            }
        });
        return ret;
    }

    @Override
    public OutputStream createResponseOutputStream() {
        return new ResteasyReactiveOutputStream(this);
    }
}
