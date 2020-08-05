package io.quarkus.qrs.runtime.core;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.jaxrs.QrsHttpHeaders;
import io.quarkus.qrs.runtime.jaxrs.QrsRequest;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class RequestContext implements Runnable, Closeable {
    private static final Logger log = Logger.getLogger(RequestContext.class);
    private final RoutingContext context;
    /**
     * The parameters array, populated by handlers
     */
    private final Object[] parameters;
    private final RuntimeResource target;
    private final RestHandler[] handlers;
    Map<String, String> pathParamValues;
    private UriInfo uriInfo;
    /**
     * The endpoint to invoke
     */
    private BeanFactory.BeanInstance<Object> endpointInstance;
    /**
     * The result of the invocation
     */
    private Object result;
    private boolean suspended = false;
    private volatile boolean running = false;
    private volatile Executor executor;
    private int position;
    private Throwable throwable;
    private QrsHttpHeaders httpHeaders;
    private ExceptionMapping exceptionMapping;
    private Serialisers serialisers;
    private Object requestEntity;
    private Map<String, Object> properties;
    private Request request;
    private EntityWriter entityWriter;

    public RequestContext(RoutingContext context, RuntimeResource target, ExceptionMapping exceptionMapping,
            Serialisers serialisers) {
        this.context = context;
        this.target = target;
        this.handlers = target.getHandlerChain();
        this.parameters = new Object[target.getParameterTypes().length];
        context.addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                close();
            }
        });
        this.exceptionMapping = exceptionMapping;
        this.serialisers = serialisers;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        resume(null, null);
    }

    public synchronized void resume(Executor executor) {
        resume(executor, null);
    }

    public synchronized void resume(Throwable throwable) {
        resume(null, throwable);
    }

    public synchronized void resume(Executor executor, Throwable throwable) {
        this.throwable = throwable;
        if (running) {
            this.executor = executor;
            if (executor == null) {
                suspended = false;
            }
        } else {
            suspended = false;
            if (executor == null) {
                ((ConnectionBase) context.request().connection()).getContext().nettyEventLoop().execute(this);
            } else {
                executor.execute(this);
            }
        }
    }

    @Override
    public void run() {
        running = true;
        try {
            if (throwable != null) {
                handleException(throwable);
                return;
            }
            while (position < handlers.length) {
                handlers[position].handle(this);
                ++position;
                if (suspended) {
                    Executor exec = null;
                    synchronized (this) {
                        if (this.executor != null) {
                            //resume happened in the meantime
                            suspended = false;
                            exec = this.executor;
                        } else if (suspended) {
                            running = false;
                            return;
                        }
                    }
                    if (exec != null) {
                        //outside sync block
                        exec.execute(this);
                        return;
                    }
                }
            }
        } catch (Throwable t) {
            handleException(t);
        } finally {
            running = false;
        }
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public QrsHttpHeaders getHttpHeaders() {
        if (httpHeaders == null) {
            httpHeaders = new QrsHttpHeaders(context.request().headers());
        }
        return httpHeaders;
    }

    public RoutingContext getContext() {
        return context;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Map<String, String> getPathParamValues() {
        return pathParamValues;
    }

    public void setPathParamValues(Map<String, String> pathParamValues) {
        this.pathParamValues = pathParamValues;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public Object getRequestEntity() {
        return requestEntity;
    }

    public RequestContext setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
        return this;
    }

    public EntityWriter getEntityWriter() {
        return entityWriter;
    }

    public RequestContext setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public Object getEndpointInstance() {
        if (endpointInstance == null) {
            return null;
        }
        return endpointInstance.getInstance();
    }

    public RequestContext setEndpointInstance(BeanFactory.BeanInstance<Object> endpointInstance) {
        this.endpointInstance = endpointInstance;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public RequestContext setResult(Object result) {
        this.result = result;
        return this;
    }

    public RuntimeResource getTarget() {
        return target;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public RequestContext setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    public RequestContext setRunning(boolean running) {
        this.running = running;
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public RequestContext setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public RequestContext setPosition(int position) {
        this.position = position;
        return this;
    }

    public RestHandler[] getHandlers() {
        return handlers;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * ATM this can only be called by the InvocationHandler
     */
    public RequestContext setThrowable(Throwable throwable) {
        invokeExceptionMapper(throwable);
        this.throwable = throwable;
        return this;
    }

    private void invokeExceptionMapper(Throwable throwable) {
        if (throwable instanceof WebApplicationException) {
            this.result = ((WebApplicationException) throwable).getResponse();
        } else {
            this.result = exceptionMapping.mapException(throwable, this);
        }
    }

    private void handleException(Throwable throwable) {
        log.error("Request failed", throwable);
        context.response().setStatusCode(500).end();
    }

    @Override
    public void close() {
        // FIXME: close filter instances somehow?
        if (endpointInstance != null) {
            endpointInstance.close();
        }
    }

    public Response getResponse() {
        return (Response) result;
    }

    public Object getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    public Collection<String> getPropertyNames() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableSet(properties.keySet());
    }

    public void setProperty(String name, Object object) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, object);
    }

    public void removeProperty(String name) {
        if (properties == null) {
            return;
        }
        properties.remove(name);
    }

    public Request getRequest() {
        if (request == null) {
            request = new QrsRequest(this);
        }
        return request;
    }
}
