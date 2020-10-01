package io.quarkus.rest.runtime.jaxrs;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class QuarkusRestAsyncResponse implements AsyncResponse, Handler<Long> {

    private final QuarkusRestRequestContext context;
    private volatile boolean suspended;
    private volatile boolean cancelled;
    private volatile TimeoutHandler timeoutHandler;
    // only used with lock, no need for volatile
    private long timerId = -1;

    public QuarkusRestAsyncResponse(QuarkusRestRequestContext context) {
        this.context = context;
        suspended = true;
    }

    @Override
    public synchronized boolean resume(Object response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        cancelTimer();
        context.setResult(response);
        context.resume();
        return true;
    }

    @Override
    public synchronized boolean resume(Throwable response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        cancelTimer();
        context.setThrowable(response);
        context.resume();
        return true;
    }

    @Override
    public boolean cancel() {
        return internalCancel(null);
    }

    @Override
    public boolean cancel(int retryAfter) {
        return internalCancel(retryAfter);
    }

    private synchronized boolean internalCancel(Object retryAfter) {
        if (cancelled) {
            return true;
        }
        if (!suspended) {
            return false;
        }
        cancelTimer();
        suspended = false;
        cancelled = true;
        ResponseBuilder response = Response.status(503);
        if (retryAfter != null)
            response.header(HttpHeaders.RETRY_AFTER, retryAfter);
        context.setThrowable(new WebApplicationException(response.build()));
        context.resume();
        return true;
    }

    @Override
    public boolean cancel(Date retryAfter) {
        return internalCancel(retryAfter);
    }

    // CALL WITH LOCK
    private void cancelTimer() {
        if (timerId != -1) {
            context.getContext().vertx().cancelTimer(timerId);
            timerId = -1;
        }
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        // we start suspended and stop being suspended on resume/cancel/timeout(which resumes) so
        // this flag is enough to know if we're done
        return !suspended;
    }

    @Override
    public synchronized boolean setTimeout(long time, TimeUnit unit) {
        if (!suspended)
            return false;
        Vertx vertx = context.getContext().vertx();
        if (timerId != -1)
            vertx.cancelTimer(timerId);
        timerId = vertx.setTimer(TimeUnit.MILLISECONDS.convert(time, unit), this);
        return true;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    @Override
    public Collection<Class<?>> register(Class<?> callback) {
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
        return null;
    }

    @Override
    public Collection<Class<?>> register(Object callback) {
        return null;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
        return null;
    }

    @Override
    public synchronized void handle(Long event) {
        // perhaps it's possible that we updated a timer and we're getting notified with the
        // previous timer we registered, in which case let's wait for the latest timer registered
        if (event.longValue() != timerId)
            return;
        // make sure we're not marked as waiting for a timer anymore
        timerId = -1;
        // if we're not suspended anymore, or we were cancelled, drop it
        if (!suspended || cancelled)
            return;
        if (timeoutHandler != null) {
            timeoutHandler.handleTimeout(this);
            // Spec says:
            // In case the time-out handler does not take any of the actions mentioned above [resume/new timeout], 
            // a default time-out strategy is executed by the runtime.
            // Stef picked to do this if the handler did not resume or set a new timeout:
            if (suspended && timerId == -1)
                resume(new ServiceUnavailableException());
        } else {
            resume(new ServiceUnavailableException());
        }
    }
}
