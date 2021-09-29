package org.jboss.resteasy.reactive.server.vertx;

import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class VertxResteasyReactiveRequestContext extends ResteasyReactiveRequestContext
        implements ServerHttpRequest, ServerHttpResponse, Handler<Void> {

    public static final String CONTINUE = "100-continue";
    protected final RoutingContext context;
    protected final HttpServerRequest request;
    protected final HttpServerResponse response;
    private final Executor contextExecutor;
    private final ClassLoader devModeTccl;
    protected Consumer<ResteasyReactiveRequestContext> preCommitTask;
    ContinueState continueState = ContinueState.NONE;

    public VertxResteasyReactiveRequestContext(Deployment deployment, ProvidersImpl providers,
            RoutingContext context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain,
            ClassLoader devModeTccl) {
        super(deployment, providers, requestContext, handlerChain, abortHandlerChain);
        this.context = context;
        this.request = context.request();
        this.response = context.response();
        this.devModeTccl = devModeTccl;
        context.addHeadersEndHandler(this);
        String expect = request.getHeader(HttpHeaderNames.EXPECT);
        ContextInternal internal = ((ConnectionBase) context.request().connection()).getContext();
        if (expect != null && expect.equalsIgnoreCase(CONTINUE)) {
            continueState = ContinueState.REQUIRED;
        }
        this.contextExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                internal.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void unused) {
                        command.run();
                    }
                });
            }
        };
    }

    @Override
    public ServerHttpResponse addCloseHandler(Runnable onClose) {
        this.response.closeHandler(new Handler<Void>() {
            @Override
            public void handle(Void v) {
                onClose.run();
            }
        });
        return this;
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

    public Executor getContextExecutor() {
        return contextExecutor;
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
        return request.method().name();
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
    public InputStream createInputStream(ByteBuffer existingData) {
        if (existingData == null) {
            return createInputStream();
        }
        return new VertxInputStream(context, 10000, Unpooled.wrappedBuffer(existingData), this);
    }

    @Override
    public InputStream createInputStream() {
        if (context.getBody() != null) {
            byte[] data = new byte[context.getBody().length()];
            context.getBody().getBytes(data);
            return new ByteArrayInputStream(data);
        }
        return new VertxInputStream(context, 10000, this);
    }

    @Override
    public ServerHttpResponse pauseRequestInput() {
        request.pause();
        return this;
    }

    @Override
    public ServerHttpResponse resumeRequestInput() {
        if (continueState == ContinueState.REQUIRED) {
            continueState = ContinueState.SENT;
            response.writeContinue();
        }
        request.resume();
        return this;
    }

    @Override
    public ServerHttpResponse setReadListener(ReadCallback callback) {
        if (context.getBody() != null) {
            callback.data(context.getBody().getByteBuf().nioBuffer());
            callback.done();
            return this;
        }
        request.pause();
        if (continueState == ContinueState.REQUIRED) {
            continueState = ContinueState.SENT;
            response.writeContinue();
        }
        request.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                if (devModeTccl != null) {
                    Thread.currentThread().setContextClassLoader(devModeTccl);
                }
                callback.data(ByteBuffer.wrap(event.getBytes()));
            }
        });
        request.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                if (devModeTccl != null) {
                    Thread.currentThread().setContextClassLoader(devModeTccl);
                }
                callback.done();
            }
        });
        request.resume();
        return this;
    }

    @Override
    public FormData getExistingParsedForm() {
        if (context.fileUploads().isEmpty() && request.formAttributes().isEmpty()) {
            return null;
        }
        FormData ret = new FormData(Integer.MAX_VALUE);
        for (var i : context.fileUploads()) {
            CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<>();
            if (i.contentType() != null) {
                headers.add(HttpHeaders.CONTENT_TYPE, i.contentType());
            }
            ret.add(i.name(), Paths.get(i.uploadedFileName()), i.fileName(), headers);
        }
        for (var i : request.formAttributes()) {
            ret.add(i.getKey(), i.getValue());
        }
        return ret;
    }

    @Override
    public boolean isOnIoThread() {
        return ((ConnectionBase) request.connection()).channel().eventLoop().inEventLoop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> theType) {
        if (theType == RoutingContext.class) {
            return (T) context;
        } else if (theType == HttpServerRequest.class) {
            return (T) request;
        } else if (theType == HttpServerResponse.class) {
            return (T) response;
        } else if (theType == ResteasyReactiveRequestContext.class) {
            return (T) this;
        }
        return null;
    }

    @Override
    public ServerHttpResponse setStatusCode(int code) {
        if (!response.headWritten()) {
            response.setStatusCode(code);
        }
        return this;
    }

    @Override
    public ServerHttpResponse end() {
        if (!response.ended()) {
            response.end();
        }
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
    public ServerHttpResponse sendFile(String path, long offset, long length) {
        response.sendFile(path, offset, length);
        return this;
    }

    @Override
    public OutputStream createResponseOutputStream() {
        return new ResteasyReactiveOutputStream(this);
    }

    @Override
    public void setPreCommitListener(Consumer<ResteasyReactiveRequestContext> task) {
        preCommitTask = task;
    }

    @Override
    public void handle(Void event) {
        if (preCommitTask != null) {
            preCommitTask.accept(this);
        }
    }

    @Override
    public ServerHttpResponse addDrainHandler(Runnable onDrain) {
        response.drainHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                onDrain.run();
            }
        });
        return this;
    }

    @Override
    public boolean isWriteQueueFull() {
        return response.writeQueueFull();
    }

    public HttpServerRequest vertxServerRequest() {
        return request;
    }

    public HttpServerResponse vertxServerResponse() {
        return response;
    }

    enum ContinueState {
        NONE,
        REQUIRED,
        SENT;
    }
}
