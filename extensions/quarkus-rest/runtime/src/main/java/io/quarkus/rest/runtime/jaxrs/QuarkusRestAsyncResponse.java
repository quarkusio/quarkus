package io.quarkus.rest.runtime.jaxrs;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class QuarkusRestAsyncResponse implements AsyncResponse {

    private final QuarkusRestRequestContext context;
    private volatile boolean suspended;
    private volatile boolean cancelled;

    public QuarkusRestAsyncResponse(QuarkusRestRequestContext context) {
        this.context = context;
        suspended = true;
    }

    @Override
    public boolean resume(Object response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        context.setResult(response);
        context.resume();
        return true;
    }

    @Override
    public boolean resume(Throwable response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        context.setThrowable(response);
        context.resume();
        return true;
    }

    @Override
    public boolean cancel() {
        if (!suspended) {
            return false;
        }
        suspended = false;
        cancelled = true;

        context.setThrowable(new WebApplicationException(Response.status(503).build()));
        context.resume();
        return true;
    }

    @Override
    public boolean cancel(int retryAfter) {
        if (!suspended) {
            return false;
        }
        suspended = false;
        cancelled = true;
        context.setThrowable(new WebApplicationException(Response.status(503)
                .header(HttpHeaders.RETRY_AFTER, retryAfter).build()));
        context.resume();
        return true;
    }

    @Override
    public boolean cancel(Date retryAfter) {
        return cancel((int) ((retryAfter.getTime() - System.currentTimeMillis()) / 1000));
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
        return !suspended || cancelled;
    }

    @Override
    public boolean setTimeout(long time, TimeUnit unit) {
        return false;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {

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
}
