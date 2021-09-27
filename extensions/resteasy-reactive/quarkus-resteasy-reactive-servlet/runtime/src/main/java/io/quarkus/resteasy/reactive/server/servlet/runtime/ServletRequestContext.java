package io.quarkus.resteasy.reactive.server.servlet.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.enterprise.event.Event;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveSecurityContext;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ResponseCommitListener;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class ServletRequestContext extends ResteasyReactiveRequestContext
        implements ServerHttpRequest, ServerHttpResponse, ResponseCommitListener {

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            ServletRequestContext::createEvent);
    final RoutingContext context;
    final HttpServletRequest request;
    final HttpServletResponse response;
    AsyncContext asyncContext;
    ServletWriteListener writeListener;
    ServletReadListener readListener;
    byte[] asyncWriteData;
    boolean closed;
    Consumer<Throwable> asyncWriteHandler;
    protected Consumer<ResteasyReactiveRequestContext> preCommitTask;

    public ServletRequestContext(Deployment deployment, ProvidersImpl providers,
            HttpServletRequest request, HttpServletResponse response,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain,
            RoutingContext context, HttpServerExchange exchange) {
        super(deployment, providers, requestContext, handlerChain, abortHandlerChain);
        this.request = request;
        this.response = response;
        this.context = context;
        exchange.addResponseCommitListener(this);
    }

    protected boolean isRequestScopeManagementRequired() {
        return asyncContext != null;
    }

    protected void beginAsyncProcessing() {
        asyncContext = request.startAsync();
    }

    @Override
    public synchronized void close() {
        if (asyncWriteData != null) {
            closed = true;
        } else {
            super.close();
            if (asyncContext != null) {
                asyncContext.complete();
            }
        }
    }

    @Override
    public ServerHttpResponse addCloseHandler(Runnable onClose) {
        context.response().closeHandler(v -> onClose.run());
        return this;
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
        return new ResteasyReactiveSecurityContext(context);
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
        return request.getHeader(name.toString());
    }

    @Override
    public Iterable<Map.Entry<String, String>> getAllRequestHeaders() {
        List<Map.Entry<String, String>> ret = new ArrayList<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            for (String v : new EnumerationIterable<>(request.getHeaders(name))) {
                ret.add(new MapEntry<>(name, v));
            }
        }
        return ret;
    }

    @Override
    public List<String> getAllRequestHeaders(String name) {
        Enumeration<String> headers = request.getHeaders(name);
        if (headers == null) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<>();
        while (headers.hasMoreElements()) {
            ret.add(headers.nextElement());
        }
        return ret;
    }

    @Override
    public boolean containsRequestHeader(CharSequence accept) {
        return request.getHeader(accept.toString()) != null;
    }

    @Override
    public String getRequestPath() {
        return request.getServletPath();
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public String getRequestNormalisedPath() {
        return context.normalizedPath();
    }

    @Override
    public String getRequestAbsoluteUri() {
        if (request.getQueryString() == null) {
            return request.getRequestURL().toString();
        } else {
            return request.getRequestURL().append("?").append(request.getQueryString()).toString();
        }
    }

    @Override
    public String getRequestScheme() {
        return request.getScheme();
    }

    @Override
    public String getRequestHost() {
        return context.request().host();
    }

    @Override
    public void closeConnection() {
        try {
            response.getOutputStream().close();
        } catch (IOException e) {
            //ignore
        }
        context.response().close();
    }

    @Override
    public String getQueryParam(String name) {
        if (!context.queryParams().contains(name)) {
            return null;
        }
        return request.getParameter(name);
    }

    @Override
    public List<String> getAllQueryParams(String name) {
        return context.queryParam(name);
    }

    @Override
    public String query() {
        return request.getQueryString();
    }

    @Override
    public Collection<String> queryParamNames() {
        return context.queryParams().names();
    }

    @Override
    public boolean isRequestEnded() {
        return context.request().isEnded();
    }

    @Override
    public InputStream createInputStream(ByteBuffer existingData) {
        return new ServletResteasyReactiveInputStream(existingData, request);
    }

    @Override
    public InputStream createInputStream() {
        try {
            return request.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServerHttpResponse pauseRequestInput() {
        //TODO
        return this;
    }

    @Override
    public ServerHttpResponse resumeRequestInput() {
        return this;
    }

    @Override
    public ServerHttpResponse setReadListener(ReadCallback callback) {
        try {
            ServletInputStream in = request.getInputStream();
            if (!request.isAsyncStarted()) {
                request.startAsync();
            }
            in.setReadListener(new ServletReadListener(in, callback));
        } catch (IOException e) {
            resume(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> theType) {
        if (theType == RoutingContext.class) {
            return (T) context;
        } else if (theType == HttpServerRequest.class) {
            return (T) context.request();
        } else if (theType == HttpServerResponse.class) {
            return (T) context.response();
        } else if (theType == HttpServletRequest.class) {
            return (T) request;
        } else if (theType == HttpServletResponse.class) {
            return (T) response;
        }
        return null;
    }

    @Override
    public ServerHttpResponse setStatusCode(int code) {
        response.setStatus(code);
        return this;
    }

    @Override
    public ServerHttpResponse end() {
        try {
            response.getOutputStream().close();
        } catch (IOException e) {
            //ignore
        }
        return this;
    }

    @Override
    public boolean headWritten() {
        return response.isCommitted();
    }

    @Override
    public ServerHttpResponse end(byte[] data) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            try {
                response.getOutputStream().write(data);
                response.getOutputStream().close();
            } catch (IOException e) {
                log.debug("IoException writing response", e);
            }
        } else {
            write(data, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    try {
                        response.getOutputStream().close();
                    } catch (IOException e) {
                        log.debug("IoException writing response", e);
                    }
                }
            });
        }
        return this;
    }

    @Override
    public ServerHttpResponse end(String data) {
        end(data.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    @Override
    public ServerHttpResponse addResponseHeader(CharSequence name, CharSequence value) {
        response.addHeader(name.toString(), value.toString());
        return this;
    }

    @Override
    public ServerHttpResponse setResponseHeader(CharSequence name, CharSequence value) {
        response.setHeader(name.toString(), value.toString());
        return this;
    }

    @Override
    public ServerHttpResponse setResponseHeader(CharSequence name, Iterable<CharSequence> values) {
        for (CharSequence v : values) {
            response.addHeader(name.toString(), v.toString());
        }
        return this;
    }

    @Override
    public Iterable<Map.Entry<String, String>> getAllResponseHeaders() {
        List<Map.Entry<String, String>> ret = new ArrayList<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String name : headerNames) {
            for (String v : response.getHeaders(name)) {
                ret.add(new MapEntry<>(name, v));
            }
        }
        return ret;
    }

    @Override
    public boolean closed() {
        return context.response().closed();
    }

    @Override
    public ServerHttpResponse setChunked(boolean chunked) {
        context.response().setChunked(chunked);
        return this;
    }

    @Override
    public ServerHttpResponse write(byte[] data, Consumer<Throwable> asyncResultHandler) {
        if (asyncWriteData != null) {
            asyncResultHandler.accept(new IllegalStateException("Cannot write before data has all been written"));
        }
        if (asyncContext == null) {
            try {
                response.getOutputStream().write(data);
                asyncResultHandler.accept(null);
            } catch (IOException e) {
                asyncResultHandler.accept(e);
            }
        } else {
            synchronized (this) {
                if (asyncWriteData != null) {
                    throw new IllegalStateException("Cannot write more than one piece of async data at a time");
                }
                asyncWriteData = data;
                asyncWriteHandler = asyncResultHandler;
                if (writeListener == null) {
                    try {
                        ServletOutputStream outputStream = response.getOutputStream();
                        outputStream.setWriteListener(writeListener = new ServletWriteListener(outputStream));
                    } catch (IOException e) {
                        asyncResultHandler.accept(e);
                    }
                } else {
                    writeListener.onWritePossible();
                }
            }
        }
        return this;
    }

    @Override
    public CompletionStage<Void> write(byte[] data) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        write(data, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                if (throwable == null) {
                    cf.complete(null);
                } else {
                    cf.completeExceptionally(throwable);
                }
            }
        });
        return cf;
    }

    @Override
    public OutputStream createResponseOutputStream() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPreCommitListener(Consumer<ResteasyReactiveRequestContext> task) {
        preCommitTask = task;
    }

    @Override
    public void beforeCommit(HttpServerExchange exchange) {
        if (preCommitTask != null) {
            preCommitTask.accept(this);
        }
    }

    class ServletWriteListener implements WriteListener {

        private final ServletOutputStream outputStream;

        ServletWriteListener(ServletOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void onWritePossible() {
            synchronized (ServletRequestContext.this) {
                if (!outputStream.isReady()) {
                    return;
                }
                Consumer<Throwable> ctx = asyncWriteHandler;
                byte[] data = asyncWriteData;
                asyncWriteHandler = null;
                asyncWriteData = null;
                try {
                    outputStream.write(data);
                    ctx.accept(null);
                } catch (IOException e) {
                    ctx.accept(e);
                }
                if (closed) {
                    close();
                }
            }
        }

        @Override
        public synchronized void onError(Throwable t) {
            synchronized (ServletRequestContext.this) {
                if (asyncWriteHandler != null) {
                    Consumer<Throwable> ctx = asyncWriteHandler;
                    asyncWriteHandler = null;
                    asyncWriteData = null;
                    ctx.accept(t);
                    close();
                }
            }
        }
    }

    class ServletReadListener implements ReadListener {

        final ServletInputStream inputStream;
        final ReadCallback readCallback;
        boolean paused;
        boolean allDone;
        Throwable problem;

        ServletReadListener(ServletInputStream inputStream, ReadCallback readCallback) {
            this.inputStream = inputStream;
            this.readCallback = readCallback;
        }

        @Override
        public void onDataAvailable() throws IOException {
            synchronized (this) {
                if (paused) {
                    return;
                }
            }
            doRead();

        }

        private void doRead() {
            if (inputStream.isReady()) {
                byte[] buf = new byte[1024];
                try {
                    int r = inputStream.read(buf);
                    readCallback.data(ByteBuffer.wrap(buf, 0, r));
                } catch (IOException e) {
                    ServletRequestContext.this.resume(problem);
                }
            }
        }

        synchronized void pause() {
            paused = true;
        }

        void resume() {
            boolean allDone;
            Throwable problem;
            synchronized (this) {
                paused = false;
                allDone = this.allDone;
                this.allDone = false;
                problem = this.problem;
                this.problem = null;
            }
            if (problem != null) {
                ServletRequestContext.this.resume(problem);
            } else if (allDone) {
                readCallback.done();
            } else {
                doRead();
            }
        }

        @Override
        public void onAllDataRead() throws IOException {
            synchronized (this) {
                if (paused) {
                    allDone = true;
                    return;
                }
            }
            readCallback.done();
        }

        @Override
        public void onError(Throwable t) {
            synchronized (this) {
                if (paused) {
                    problem = t;
                    return;
                }
            }
            ServletRequestContext.this.resume(t);
        }
    }

    static final class MapEntry<K, V> implements Map.Entry<K, V> {

        private final K key;
        private V value;

        MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = value;
            this.value = value;
            return old;
        }
    }

    @Override
    public ServerHttpResponse sendFile(String path, long offset, long length) {
        context.response().sendFile(path, offset, length);
        return this;
    }

    @Override
    public boolean isWriteQueueFull() {
        return context.response().writeQueueFull();
    }

    @Override
    public ServerHttpResponse addDrainHandler(Runnable onDrain) {
        context.response().drainHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                onDrain.run();
            }
        });
        return this;
    }
}
