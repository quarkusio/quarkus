package org.jboss.resteasy.reactive.server.vertx;

import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class VertxResteasyReactiveRequestContext extends ResteasyReactiveRequestContext
        implements ServerHttpRequest, ServerHttpResponse {

    protected final RoutingContext context;
    protected final HttpServerRequest request;
    protected final HttpServerResponse response;

    public VertxResteasyReactiveRequestContext(Deployment deployment, ProvidersImpl providers,
            RoutingContext context,
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

    @Override
    protected EventLoop getEventLoop() {
        return ((ConnectionBase) context.request().connection()).channel().eventLoop();
    }

    @Override
    public Runnable registerTimer(long millis, Runnable task) {
        ScheduledFuture<?> handle = getEventLoop().schedule(task, millis, TimeUnit.MILLISECONDS);
        return new Runnable() {
            @Override
            public void run() {
                handle.cancel(false);
            }
        };
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
        response.close();
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
    public InputStream createInputStream(ByteBuffer existingData) {
        return new VertxInputStream(context, 10000, Unpooled.wrappedBuffer(existingData));
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
                callback.data(ByteBuffer.wrap(event.getBytes()));
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
    public <T> T unwrap(Class<T> theType) {
        if (theType == RoutingContext.class) {
            return (T) context;
        } else if (theType == HttpServerRequest.class) {
            return (T) request;
        } else if (theType == HttpServerResponse.class) {
            return (T) response;
        }
        return null;
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
