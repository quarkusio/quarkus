package io.quarkus.rest.server.servlet.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.common.http.ServerHttpRequest;
import org.jboss.resteasy.reactive.common.http.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.core.QuarkusRestDeployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.rest.server.runtime.ResteasyReactiveSecurityContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class QuarkusServletRequestContext extends ResteasyReactiveRequestContext
        implements ServerHttpRequest, ServerHttpResponse {

    private static final LazyValue<Event<SecurityIdentity>> SECURITY_IDENTITY_EVENT = new LazyValue<>(
            QuarkusServletRequestContext::createEvent);
    final RoutingContext context;
    final HttpServletRequest request;
    final HttpServletResponse response;
    AsyncContext asyncContext;
    ServletWriteListener writeListener;
    byte[] asyncWriteData;
    Consumer<Throwable> asyncWriteHandler;

    public QuarkusServletRequestContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers,
            HttpServletRequest request, HttpServletResponse response,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain,
            RoutingContext context) {
        super(deployment, providers, requestContext, handlerChain, abortHandlerChain);
        this.request = request;
        this.response = response;
        this.context = context;
    }

    protected boolean isRequestScopeManagementRequired() {
        return asyncContext != null;
    }

    protected void beginAsyncProcessing() {
        asyncContext = request.startAsync();
    }

    @Override
    public void close() {
        super.close();
        if (asyncContext != null) {
            asyncContext.complete();
        }
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
        return context.normalisedPath();
    }

    @Override
    public String getRequestAbsoluteUri() {
        return request.getRequestURI();
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
    public String getFormAttribute(String name) {
        if (context.queryParams().contains(name)) {
            return null;
        }
        return request.getParameter(name);
    }

    @Override
    public List<String> getAllFormAttributes(String name) {
        if (context.queryParams().contains(name)) {
            return Collections.emptyList();
        }
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(values);
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
    public void setExpectMultipart(boolean expectMultipart) {
        //read the form data
        request.getParameterMap();
    }

    @Override
    public InputStream createInputStream(ByteBuffer existingData) {
        return new QuarkusRestServletInputStream(existingData, request);
    }

    @Override
    public ServerHttpResponse pauseRequestInput() {
        //TODO
        return this;
    }

    @Override
    public ServerHttpResponse resumeRequestInput() {
        //TODO
        return this;
    }

    @Override
    public ServerHttpResponse setReadListener(ReadCallback callback) {
        byte[] buf = new byte[1024];
        int r;
        try {
            InputStream in = request.getInputStream();
            while ((r = in.read(buf)) > 0) {
                callback.data(ByteBuffer.wrap(buf, 0, r));
            }
            callback.done();
        } catch (IOException e) {
            resume(e);
        }
        return this;
    }

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
        try {
            response.getOutputStream().write(data);
            response.getOutputStream().close();
        } catch (IOException e) {
            log.debug("IoException writing response", e);
        }
        return this;
    }

    @Override
    public ServerHttpResponse end(String data) {
        try {
            response.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().close();
        } catch (IOException e) {
            log.debug("IoException writing response", e);
        }
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
            if (writeListener == null) {
                try {
                    ServletOutputStream outputStream = response.getOutputStream();
                    outputStream.setWriteListener(writeListener = new ServletWriteListener(outputStream));
                } catch (IOException e) {
                    asyncResultHandler.accept(e);
                }
            } else {
                asyncWriteData = data;
                asyncWriteHandler = asyncResultHandler;
                writeListener.onWritePossible();
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

    class ServletWriteListener implements WriteListener {

        private final ServletOutputStream outputStream;

        ServletWriteListener(ServletOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public synchronized void onWritePossible() {
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
        }

        @Override
        public synchronized void onError(Throwable t) {
            if (asyncWriteHandler != null) {
                Consumer<Throwable> ctx = asyncWriteHandler;
                asyncWriteHandler = null;
                asyncWriteData = null;
                ctx.accept(t);
            }
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
}
