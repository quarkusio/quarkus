package org.jboss.resteasy.reactive.common.core;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ConnectionCallback;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.PreserveTargetException;
import org.jboss.resteasy.reactive.spi.RestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public abstract class AbstractResteasyReactiveContext<T extends AbstractResteasyReactiveContext<T, H>, H extends RestHandler<T>>
        implements Runnable, Closeable, ResteasyReactiveCallbackContext {
    protected static final Logger log = Logger.getLogger(AbstractResteasyReactiveContext.class);
    protected H[] handlers;
    protected H[] abortHandlerChain;
    protected int position;
    protected Throwable throwable;
    private boolean suspended = false;
    private volatile boolean requestScopeActivated = false;
    private boolean running = false;
    private volatile Executor executor; // ephemerally set by handlers to signal that we resume, it needs to be on this executor
    private volatile Executor lastExecutor; // contains the last executor which was provided during resume - needed to submit there if suspended again
    // This is used to store properties used in the various JAX-RS context objects, but it also stores some of the properties of this
    // object that are very infrequently accessed. This is done in order to cut down the size of this object in order to take advantage of better caching
    private Map<String, Object> properties;
    private final ThreadSetupAction requestContext;
    private ThreadSetupAction.ThreadState currentRequestScope;
    private List<CompletionCallback> completionCallbacks;
    private boolean abortHandlerChainStarted;

    private boolean closed = false;

    public AbstractResteasyReactiveContext(H[] handlerChain, H[] abortHandlerChain, ThreadSetupAction requestContext) {
        this.handlers = handlerChain;
        this.abortHandlerChain = abortHandlerChain;
        this.requestContext = requestContext;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        resume((Executor) null);
    }

    public synchronized void resume(Throwable throwable) {
        resume(throwable, false);
    }

    public synchronized void resume(Throwable throwable, boolean keepTarget) {
        handleException(throwable, keepTarget);
        resume((Executor) null);
    }

    public synchronized void resume(Executor executor) {
        if (running) {
            this.executor = executor;
            if (executor == null) {
                suspended = false;
            } else {
                this.lastExecutor = executor;
            }
        } else {
            suspended = false;
            if (executor == null) {
                if (lastExecutor == null) {
                    // TODO CES - Ugly Ugly hack!
                    Executor ctxtExecutor = getContextExecutor();
                    if (ctxtExecutor == null) {
                        // Won't use the TCCL.
                        getEventLoop().execute(this);
                    } else {
                        // Use the TCCL.
                        ctxtExecutor.execute(this);
                    }
                } else {
                    lastExecutor.execute(this);
                }
            } else {
                executor.execute(this);
            }
        }
    }

    public H[] getAbortHandlerChain() {
        return abortHandlerChain;
    }

    public T setAbortHandlerChain(H[] abortHandlerChain) {
        this.abortHandlerChain = abortHandlerChain;
        return (T) this;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        //TODO: do we even have any other resources to close?
        if (currentRequestScope != null) {
            currentRequestScope.close();
        }
        onComplete(throwable);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    protected abstract Executor getEventLoop();

    public Executor getContextExecutor() {
        return null;
    }

    protected boolean isRequestScopeManagementRequired() {
        return true;
    }

    @Override
    public void run() {
        running = true;
        boolean processingSuspended = false;
        //if this is a blocking target we don't activate for the initial non-blocking part
        //unless there are pre-mapping filters as these may require CDI
        boolean disassociateRequestScope = false;
        boolean aborted = false;
        try {
            while (position < handlers.length) {
                int pos = position;
                position++; //increment before, as reset may reset it to zero
                try {
                    invokeHandler(pos);
                    if (suspended) {
                        synchronized (this) {
                            // as running is not volatile but instead read from inside the same monitor,
                            // we write it from inside this monitor as well to ensure
                            // that the read is visible regardless of the reading thread
                            running = true;
                            if (isRequestScopeManagementRequired()) {
                                if (requestScopeActivated) {
                                    disassociateRequestScope = true;
                                    requestScopeActivated = false;
                                }
                            } else {
                                requestScopeActivated = false;
                                requestScopeDeactivated();
                            }
                            if (suspended) {
                                processingSuspended = true;
                                return;
                            }
                        }
                    }

                } catch (Throwable t) {
                    aborted = abortHandlerChainStarted;
                    if (t instanceof PreserveTargetException) {
                        handleException(t.getCause(), true);
                    } else {
                        handleException(t, true);
                    }
                    if (aborted) {
                        return;
                    } else if (suspended) {
                        log.error("Uncaught exception thrown when the request context is suspended. " +
                                " Resuming the request to unlock processing the error." +
                                " This may not be appropriate in your situation, please resume the request context when this exception is thrown. ",
                                t);
                        resume(t);
                    }
                }
            }
        } catch (Throwable t) {
            handleUnrecoverableError(t);
        } finally {
            // we need to make sure we don't close the underlying stream in the event loop if the task
            // has been offloaded to the executor
            if ((position == handlers.length && !processingSuspended) || aborted) {
                if (requestScopeActivated) {
                    requestScopeDeactivated();
                    currentRequestScope.deactivate();
                }
                close();
            } else {
                if (disassociateRequestScope) {
                    requestScopeDeactivated();
                    currentRequestScope.deactivate();
                }
                beginAsyncProcessing();
                Executor exec = null;
                boolean resumed = false;
                synchronized (this) {
                    running = false;
                    if (this.executor != null) {
                        //resume happened in the meantime
                        suspended = false;
                        exec = this.executor;
                        // prevent future suspensions from re-submitting the task
                        this.executor = null;
                    } else if (!suspended) {
                        resumed = true;
                    }
                }
                if (exec != null) {
                    //outside sync block
                    exec.execute(this);
                } else if (resumed) {
                    resume();
                }

            }
        }
    }

    protected void invokeHandler(int pos) throws Exception {
        handlers[pos].handle((T) this);
    }

    protected void beginAsyncProcessing() {

    }

    protected void requestScopeDeactivated() {

    }

    /**
     * Ensures the CDI request scope is running when inside a handler chain
     */
    public void requireCDIRequestScope() {
        if (requestScopeActivated) {
            return;
        }
        requestScopeActivated = true;
        if (isRequestScopeManagementRequired()) {
            if (currentRequestScope == null) {
                currentRequestScope = requestContext.activateInitial();
            } else {
                currentRequestScope.activate();
            }
        } else {
            currentRequestScope = requestContext.currentState();
        }
        handleRequestScopeActivation();
    }

    /**
     * Captures the CDI request scope for use outside of handler chains.
     */
    public ThreadSetupAction.ThreadState captureCDIRequestScope() {
        requireCDIRequestScope();
        return currentRequestScope;
    }

    protected abstract void handleRequestScopeActivation();

    /**
     * Restarts handler chain processing on a chain that does not target a specific resource
     * <p>
     * Generally used to abort processing.
     *
     * @param newHandlerChain The new handler chain
     */
    public void restart(H[] newHandlerChain) {
        restart(newHandlerChain, false);
    }

    public void restart(H[] newHandlerChain, boolean keepTarget) {
        this.handlers = newHandlerChain;
        position = 0;
        restarted(keepTarget);
    }

    protected abstract void restarted(boolean keepTarget);

    public boolean isSuspended() {
        return suspended;
    }

    public T setSuspended(boolean suspended) {
        this.suspended = suspended;
        return (T) this;
    }

    public int getPosition() {
        return position;
    }

    public T setPosition(int position) {
        this.position = position;
        return (T) this;
    }

    public H[] getHandlers() {
        return handlers;
    }

    /**
     * If we are on the abort chain already, send a 500. If not, turn the throwable into
     * a response result and switch to the abort chain
     */
    public void handleException(Throwable t) {
        handleException(t, false);
    }

    public void handleException(Throwable t, boolean keepSameTarget) {
        if (abortHandlerChainStarted) {
            log.debug("Attempting to handle unrecoverable exception", t);
            handleUnrecoverableError(unwrapException(t));
        } else {
            this.throwable = unwrapException(t);
            log.debug("Restarting handler chain for exception exception", this.throwable);
            abortHandlerChainStarted = true;
            restart(abortHandlerChain, keepSameTarget);
        }
    }

    protected Throwable unwrapException(Throwable t) {
        if (t instanceof UnwrappableException) {
            return t.getCause();
        }

        return t;
    }

    protected abstract void handleUnrecoverableError(Throwable throwable);

    protected static final String CUSTOM_RR_PROPERTIES_PREFIX = "$RR$";

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
        Set<String> result = new HashSet<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(CUSTOM_RR_PROPERTIES_PREFIX)) {
                continue;
            }
            result.add(key);
        }
        return Collections.unmodifiableSet(result);
    }

    public void setProperty(String name, Object object) {
        if (object == null) {
            removeProperty(name);
            return;
        }
        if (properties == null) {
            synchronized (this) {
                if (properties == null) {
                    properties = new ConcurrentHashMap<>();
                }
            }
        }
        properties.put(name, object);
    }

    public void removeProperty(String name) {
        if (properties == null) {
            return;
        }
        properties.remove(name);
    }

    synchronized void onComplete(Throwable throwable) {
        if (completionCallbacks != null) {
            for (CompletionCallback callback : completionCallbacks) {
                callback.onComplete(throwable);
            }
        }
    }

    @Override
    public synchronized void registerCompletionCallback(CompletionCallback callback) {
        if (completionCallbacks == null)
            completionCallbacks = new ArrayList<>();
        completionCallbacks.add(callback);
    }

    private static final String CONNECTION_CALLBACK_PROPERTY_KEY = CUSTOM_RR_PROPERTIES_PREFIX + "CONNECTION_CALLBACK";

    @Override
    public synchronized void registerConnectionCallback(ConnectionCallback callback) {
        List<ConnectionCallback> connectionCallbacks = (List<ConnectionCallback>) getProperty(CONNECTION_CALLBACK_PROPERTY_KEY);
        if (connectionCallbacks == null) {
            connectionCallbacks = new ArrayList<>();
            setProperty(CONNECTION_CALLBACK_PROPERTY_KEY, connectionCallbacks);
        }
        connectionCallbacks.add(callback);
    }

    public void setAbortHandlerChainStarted(boolean value) {
        abortHandlerChainStarted = value;
    }
}
