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

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.jaxrs.QrsContainerRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsHttpHeaders;
import io.quarkus.qrs.runtime.jaxrs.QrsRequest;
import io.quarkus.qrs.runtime.jaxrs.QrsUriInfo;
import io.quarkus.qrs.runtime.mapping.RuntimeResource;
import io.quarkus.qrs.runtime.spi.BeanFactory;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class QrsRequestContext implements Runnable, Closeable {
    private static final Logger log = Logger.getLogger(QrsRequestContext.class);
    public static final Object[] EMPTY_ARRAY = new Object[0];
    private final QrsDeployment deployment;
    private final RoutingContext context;
    private final ManagedContext requestContext;
    private final CurrentVertxRequest currentVertxRequest;
    private InjectableContext.ContextState currentRequestScope;
    /**
     * The parameters array, populated by handlers
     */
    private Object[] parameters;
    private RuntimeResource target;
    private RestHandler[] handlers;
    private Map<String, String> pathParamValues;
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
    private Object requestEntity;
    private Map<String, Object> properties;
    private Request request;
    private EntityWriter entityWriter;
    private QrsContainerRequestContext containerRequestContext;
    private String method;

    public QrsRequestContext(QrsDeployment deployment, RoutingContext context, ManagedContext requestContext,
            CurrentVertxRequest currentVertxRequest, RuntimeResource target) {
        this.deployment = deployment;
        this.context = context;
        this.requestContext = requestContext;
        this.currentVertxRequest = currentVertxRequest;
        this.target = target;
        this.handlers = target.getHandlerChain();
        this.parameters = new Object[target.getParameterTypes().length];
        context.addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                close();
            }
        });
    }

    public QrsRequestContext(QrsDeployment deployment, RoutingContext context, ManagedContext requestContext,
            CurrentVertxRequest currentVertxRequest, RestHandler... handlerChain) {
        this.deployment = deployment;
        this.context = context;
        this.requestContext = requestContext;
        this.currentVertxRequest = currentVertxRequest;
        this.handlers = handlerChain;
        this.parameters = EMPTY_ARRAY;
        context.addEndHandler(new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                close();
            }
        });
    }

    public QrsDeployment getDeployment() {
        return deployment;
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
        //if this is a blocking target we don't activate for the initial non-blocking part
        //unless there are pre-mapping filters as these may require CDI
        boolean activationRequired = target == null || (target.isBlocking() && executor == null);
        if (activationRequired) {
            if (currentRequestScope == null) {
                requestContext.activate();
                currentVertxRequest.setCurrent(context);
            } else {
                requestContext.activate(currentRequestScope);
            }
        }
        try {
            if (throwable != null) {
                handleException(throwable);
                return;
            }
            while (position < handlers.length) {
                int pos = position;
                position++; //increment before, as reset may reset it to zero
                handlers[pos].handle(this);
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
            if (activationRequired) {
                if (position == handlers.length) {
                    requestContext.terminate();
                } else {
                    currentRequestScope = requestContext.getState();
                    requestContext.deactivate();
                }
            }
        }
    }

    /**
     * Restarts handler chain processing on a chain that does not target a specific resource
     *
     * Generally used to abort processing.
     *
     * @param newHandlerChain The new handler chain
     */
    public void restart(RestHandler[] newHandlerChain) {
        this.handlers = newHandlerChain;
        position = 0;
        parameters = new Object[0];
        target = null;
    }

    /**
     * Restarts handler chain processing with a new chain targeting a new resource.
     *
     * @param target The resource target
     */
    public void restart(RuntimeResource target) {
        this.handlers = target.getHandlerChain();
        position = 0;
        parameters = new Object[target.getParameterTypes().length];
        this.target = target;
    }

    /**
     * Resets the build time serialization assumptions. Called if a filter
     * modifies the response
     */
    public void resetBuildTimeSerialization() {
        entityWriter = deployment.getDynamicEntityWriter();
    }

    public UriInfo getUriInfo() {
        if (uriInfo == null) {
            uriInfo = new QrsUriInfo(context.request().absoluteURI(), "/");
        }
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

    public QrsRequestContext setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
        return this;
    }

    public EntityWriter getEntityWriter() {
        return entityWriter;
    }

    public QrsRequestContext setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public Object getEndpointInstance() {
        if (endpointInstance == null) {
            return null;
        }
        return endpointInstance.getInstance();
    }

    public QrsRequestContext setEndpointInstance(BeanFactory.BeanInstance<Object> endpointInstance) {
        this.endpointInstance = endpointInstance;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public QrsRequestContext setResult(Object result) {
        this.result = result;
        return this;
    }

    public RuntimeResource getTarget() {
        return target;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public QrsRequestContext setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    public QrsRequestContext setRunning(boolean running) {
        this.running = running;
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public QrsRequestContext setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public QrsRequestContext setPosition(int position) {
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
    public QrsRequestContext setThrowable(Throwable throwable) {
        invokeExceptionMapper(throwable);
        this.throwable = throwable;
        return this;
    }

    private void invokeExceptionMapper(Throwable throwable) {
        if (throwable instanceof WebApplicationException) {
            this.result = ((WebApplicationException) throwable).getResponse();
        } else {
            this.result = deployment.getExceptionMapping().mapException(throwable, this);
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

    public QrsContainerRequestContext getContainerRequestContext() {
        if (containerRequestContext == null) {
            containerRequestContext = new QrsContainerRequestContext(this);
        }
        return containerRequestContext;
    }

    public String getMethod() {
        if (method == null) {
            return context.request().rawMethod();
        }
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
