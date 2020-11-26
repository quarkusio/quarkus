package io.quarkus.rest.server.test.resteasy.async.filters;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.core.QuarkusRestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestFilter;

public abstract class AsyncRequestFilter implements QuarkusRestContainerRequestFilter {

    private final String name;
    private volatile String callbackException;
    private static final Logger LOG = Logger.getLogger(AsyncRequestFilter.class);

    public AsyncRequestFilter(final String name) {
        this.name = name;
    }

    @Override
    public void filter(QuarkusRestContainerRequestContext ctx) {
        ctx.getHeaders().add("RequestFilterCallback" + name, String.valueOf(callbackException));
        callbackException = null;

        String action = ctx.getHeaderString(name);
        LOG.error("Filter request for " + name + " with action: " + action);
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
            QuarkusRestContext quarkusRestContext = ctx.getQuarkusRestContext();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Error:", e);
                }
                quarkusRestContext.registerCompletionCallback((t) -> {
                    if (callbackException != null)
                        throw new RuntimeException("Callback called twice");
                    callbackException = Objects.toString(t);
                });
                if ("true".equals(ctx.getHeaderString("UseExceptionMapper")))
                    ctx.resume(new AsyncFilterException("ouch"));
                else
                    ctx.resume(new Throwable("ouch"));
            });
        }
        LOG.error("Filter request for " + name + " with action: " + action + " done");
    }

}
