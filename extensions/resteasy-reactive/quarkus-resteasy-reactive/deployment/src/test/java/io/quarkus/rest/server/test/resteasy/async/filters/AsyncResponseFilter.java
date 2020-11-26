package io.quarkus.rest.server.test.resteasy.async.filters;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.core.QuarkusRestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

public abstract class AsyncResponseFilter implements QuarkusRestContainerResponseFilter {

    private final String name;
    private volatile String callbackException;
    private static final Logger LOG = Logger.getLogger(AsyncRequestFilter.class);

    public AsyncResponseFilter(final String name) {
        this.name = name;
    }

    @Override
    public void filter(ResteasyReactiveContainerRequestContext requestContext, ContainerResponseContext ctx) {
        // copy request filter callback values
        for (Entry<String, List<String>> entry : requestContext.getHeaders().entrySet()) {
            if (entry.getKey().startsWith("RequestFilterCallback"))
                addValuesToContext(ctx, entry);
        }
        ctx.getHeaders().add("ResponseFilterCallback" + name, String.valueOf(callbackException));
        callbackException = null;

        String action = requestContext.getHeaderString(name);
        LOG.error("Filter response for " + name + " with action: " + action);
        if ("sync-pass".equals(action)) {
            // do nothing
        } else if ("sync-fail".equals(action)) {
            ctx.setEntity(name);
        } else if ("async-pass".equals(action)) {
            requestContext.suspend();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> requestContext.resume());
        } else if ("async-pass-instant".equals(action)) {
            requestContext.suspend();
            requestContext.resume();
        } else if ("async-fail".equals(action)) {
            requestContext.suspend();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                ctx.setEntity(name);
                requestContext.resume();
            });
        } else if ("async-fail-late".equals(action)) {
            requestContext.suspend();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Error:", e);
                }
                ctx.setEntity(name);
                requestContext.resume();
            });
        } else if ("async-fail-instant".equals(action)) {
            requestContext.suspend();
            ctx.setEntity(name);
            requestContext.resume();
        } else if ("sync-throw".equals(action)) {
            throw new AsyncFilterException("ouch");
        } else if ("async-throw-late".equals(action)) {
            requestContext.suspend();
            QuarkusRestContext quarkusRestContext = requestContext.getQuarkusRestContext();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Error:", e);
                }
                ctx.setEntity(name);
                quarkusRestContext.registerCompletionCallback((t) -> {
                    if (callbackException != null)
                        throw new RuntimeException("Callback called twice");
                    callbackException = Objects.toString(t);
                });
                if ("true".equals(requestContext.getHeaderString("UseExceptionMapper")))
                    requestContext.resume(new AsyncFilterException("ouch"));
                else
                    requestContext.resume(new Throwable("ouch"));
            });
        }
        LOG.error("Filter response for " + name + " with action: " + action + " done");
    }

    @SuppressWarnings("unchecked")
    private void addValuesToContext(ContainerResponseContext responseContext, Entry<String, List<String>> entry) {
        // cast required to disambiguate with Object... method
        responseContext.getHeaders().addAll(entry.getKey(), (List) entry.getValue());
    }
}
