package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;

public abstract class AsyncRequestFilter implements ResteasyReactiveContainerRequestFilter {

    private final String name;
    private volatile String callbackException;
    private static final Logger LOG = Logger.getLogger(AsyncRequestFilter.class);

    public AsyncRequestFilter(final String name) {
        this.name = name;
    }

    @Override
    public void filter(ResteasyReactiveContainerRequestContext ctx) {
        ctx.getHeaders().add("RequestFilterCallback" + name, String.valueOf(callbackException));
        callbackException = null;

        String action = ctx.getHeaderString(name);
        LOG.debug("Filter request for " + name + " with action: " + action);
        if ("sync-pass".equals(action)) {
            // do nothing
        } else if ("sync-fail".equals(action)) {
            ctx.abortWith(Response.ok(name).build());
        } else if ("async-pass".equals(action)) {
            ctx.suspend();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> ctx.resume());
        } else if ("async-pass-instant".equals(action)) {
            ctx.suspend();
            ctx.resume();
        } else if ("async-fail".equals(action)) {
            ctx.suspend();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> ctx.abortWith(Response.ok(name).build()));
        } else if ("async-fail-instant".equals(action)) {
            ctx.suspend();
            ctx.abortWith(Response.ok(name).build());
        } else if ("async-throw-late".equals(action)) {
            ctx.suspend();
            ServerRequestContext resteasyReactiveCallbackContext = ctx.getServerRequestContext();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOG.debug("Error:", e);
                }
                resteasyReactiveCallbackContext.registerCompletionCallback((t) -> {
                    if (callbackException != null)
                        throw ExceptionUtil.removeStackTrace(new RuntimeException("Callback called twice"));
                    callbackException = Objects.toString(t);
                });
                if ("true".equals(ctx.getHeaderString("UseExceptionMapper")))
                    ctx.resume(ExceptionUtil.removeStackTrace(new AsyncFilterException("ouch")));
                else
                    ctx.resume(ExceptionUtil.removeStackTrace(new Throwable("ouch")));
            });
        }
        LOG.debug("Filter request for " + name + " with action: " + action + " done");
    }

}
