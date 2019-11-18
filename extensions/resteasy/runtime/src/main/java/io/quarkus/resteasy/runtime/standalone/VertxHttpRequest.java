package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.core.AbstractAsynchronousResponse;
import org.jboss.resteasy.core.AbstractExecutionContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.BaseHttpRequest;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;

import io.quarkus.arc.ManagedContext;
import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstraction for an inbound http request on the server, or a response from a server to a client
 * <p>
 * We have this abstraction so that we can reuse marshalling objects in a client framework and serverside framework
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Norman Maurer
 * @author Kristoffer Sjogren
 * @version $Revision: 1 $
 */
public final class VertxHttpRequest extends BaseHttpRequest {
    private ResteasyHttpHeaders httpHeaders;
    private String httpMethod;
    private LazyHostSupplier remoteHost;
    private InputStream inputStream;
    private VertxHttpResponse response;
    private VertxExecutionContext executionContext;
    private final RoutingContext routingContext;
    private final Context context;
    private final ManagedContext requestContext;
    private final ManagedContext.ContextState requestContextState;

    public VertxHttpRequest(Context context,
            RoutingContext routingContext,
            ResteasyHttpHeaders httpHeaders,
            ResteasyUriInfo uri,
            String httpMethod,
            LazyHostSupplier remoteHost,
            SynchronousDispatcher dispatcher,
            VertxHttpResponse response, ManagedContext requestContext) {
        super(uri);
        this.context = context;
        this.response = response;
        this.httpHeaders = httpHeaders;
        this.httpMethod = httpMethod;
        this.remoteHost = remoteHost;
        this.executionContext = new VertxExecutionContext(this, response, dispatcher);
        this.requestContext = requestContext;
        this.requestContextState = requestContext.getState();
        this.routingContext = routingContext;
    }

    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        return httpHeaders.getMutableHeaders();
    }

    @Override
    public void setHttpMethod(String method) {
        this.httpMethod = method;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        final Map<String, Object> attributes = routingContext.data();
        if (attributes == null) {
            return Collections.emptyEnumeration();
        } else {
            Enumeration<String> en = new Enumeration<String>() {
                private Iterator<String> it = attributes.keySet().iterator();

                @Override
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                @Override
                public String nextElement() {
                    return it.next();
                }
            };
            return en;
        }
    }

    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        return executionContext;
    }

    @Override
    public Object getAttribute(String attribute) {
        return routingContext.get(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        routingContext.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        routingContext.remove(name);
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost.getRemoteHost();
    }

    @Override
    public String getRemoteAddress() {
        return remoteHost.getRemoteHost();
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream stream) {
        this.inputStream = stream;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    public VertxHttpResponse getResponse() {
        return response;
    }

    @Override
    public void forward(String path) {
        throw new NotImplementedYetException();
    }

    @Override
    public boolean wasForwarded() {
        return false;
    }

    class VertxExecutionContext extends AbstractExecutionContext {
        protected final VertxHttpRequest request;
        protected final VertxHttpResponse response;
        protected volatile boolean done;
        protected volatile boolean cancelled;
        protected volatile boolean wasSuspended;
        protected VertxHttpAsyncResponse asyncResponse;

        VertxExecutionContext(final VertxHttpRequest request, final VertxHttpResponse response,
                final SynchronousDispatcher dispatcher) {
            super(dispatcher, request, response);
            this.request = request;
            this.response = response;
            this.asyncResponse = new VertxHttpAsyncResponse(dispatcher, request, response);
        }

        @Override
        public boolean isSuspended() {
            return wasSuspended;
        }

        @Override
        public ResteasyAsynchronousResponse getAsyncResponse() {
            return asyncResponse;
        }

        @Override
        public ResteasyAsynchronousResponse suspend() throws IllegalStateException {
            return suspend(-1);
        }

        @Override
        public ResteasyAsynchronousResponse suspend(long millis) throws IllegalStateException {
            return suspend(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public ResteasyAsynchronousResponse suspend(long time, TimeUnit unit) throws IllegalStateException {
            if (wasSuspended) {
                throw new IllegalStateException("Request already suspended");
            }
            wasSuspended = true;
            return asyncResponse;
        }

        @Override
        public void complete() {
            if (wasSuspended && asyncResponse != null)
                asyncResponse.complete();
        }

        /**
         * Vertx implementation of {@link AsyncResponse}.
         *
         * @author Kristoffer Sjogren
         */
        class VertxHttpAsyncResponse extends AbstractAsynchronousResponse {
            private final Object responseLock = new Object();
            private long timerID = -1;
            private VertxHttpResponse vertxResponse;

            VertxHttpAsyncResponse(final SynchronousDispatcher dispatcher, final VertxHttpRequest request,
                    final VertxHttpResponse response) {
                super(dispatcher, request, response);
                this.vertxResponse = response;
            }

            @Override
            public void initialRequestThreadFinished() {
                // done
            }

            @Override
            public void complete() {
                synchronized (responseLock) {
                    if (done || cancelled) {
                        return;
                    }
                    done = true;
                    requestContext.activate(requestContextState);
                    requestContext.terminate();
                    vertxFlush();
                }
            }

            @Override
            public boolean resume(Object entity) {
                synchronized (responseLock) {
                    if (done)
                        return false;
                    if (cancelled)
                        return false;
                    done = true;
                    requestContext.activate(requestContextState);
                    return internalResume(entity, new FlushTask());
                }
            }

            @Override
            public boolean resume(Throwable ex) {
                synchronized (responseLock) {
                    if (done)
                        return false;
                    if (cancelled)
                        return false;
                    done = true;
                    requestContext.activate(requestContextState);
                    return internalResume(ex, new FlushTask());
                }
            }

            @Override
            public boolean cancel() {
                synchronized (responseLock) {
                    if (cancelled) {
                        return true;
                    }
                    if (done) {
                        return false;
                    }
                    done = true;
                    cancelled = true;
                    requestContext.activate(requestContextState);
                    return internalResume(Response.status(Response.Status.SERVICE_UNAVAILABLE).build(), new FlushTask());
                }
            }

            @Override
            public boolean cancel(int retryAfter) {
                synchronized (responseLock) {
                    if (cancelled)
                        return true;
                    if (done)
                        return false;
                    done = true;
                    cancelled = true;
                    requestContext.activate(requestContextState);
                    return internalResume(
                            Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter)
                                    .build(),
                            new FlushTask());
                }
            }

            protected synchronized void vertxFlush() {
                try {
                    vertxResponse.finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean cancel(Date retryAfter) {
                synchronized (responseLock) {
                    if (cancelled)
                        return true;
                    if (done)
                        return false;
                    done = true;
                    cancelled = true;
                    requestContext.activate(requestContextState);
                    try {
                        return internalResume(
                                Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter)
                                        .build(),
                                t -> vertxFlush());
                    } finally {
                        requestContext.terminate();
                    }
                }
            }

            @Override
            public boolean isSuspended() {
                return !done && !cancelled;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                return done;
            }

            @Override
            public boolean setTimeout(long time, TimeUnit unit) {
                synchronized (responseLock) {
                    if (done || cancelled)
                        return false;
                    if (timerID > -1 && !context.owner().cancelTimer(timerID)) {
                        return false;
                    }
                    timerID = context.owner().setTimer(unit.toMillis(time), v -> handleTimeout());
                }
                return true;
            }

            protected void handleTimeout() {
                if (timeoutHandler != null) {
                    timeoutHandler.handleTimeout(this);
                }
                if (done)
                    return;
                resume(new ServiceUnavailableException());
            }

            private class FlushTask implements Consumer<Throwable> {
                @Override
                public void accept(Throwable t) {
                    try {
                        requestContext.terminate();
                    } finally {
                        VertxHttpAsyncResponse.this.vertxFlush();
                    }
                }
            }
        }
    }
}
