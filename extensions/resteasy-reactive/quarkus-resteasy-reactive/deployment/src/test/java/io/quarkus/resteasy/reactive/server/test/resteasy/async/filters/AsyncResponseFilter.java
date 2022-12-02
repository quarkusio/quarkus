package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;

public abstract class AsyncResponseFilter implements ResteasyReactiveContainerResponseFilter {

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
        LOG.debug("Filter response for " + name + " with action: " + action);
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
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    LOG.debug("Error:", e);
                }
                ctx.setEntity(name);
                requestContext.resume();
            });
        } else if ("async-fail-instant".equals(action)) {
            requestContext.suspend();
            ctx.setEntity(name);
            requestContext.resume();
        } else if ("sync-throw".equals(action)) {
            throw ExceptionUtil.removeStackTrace(new AsyncFilterException("ouch"));
        } else if ("async-throw-late".equals(action)) {
            requestContext.suspend();
            ServerRequestContext resteasyReactiveCallbackContext = requestContext.getServerRequestContext();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOG.debug("Error:", e);
                }
                ctx.setEntity(name);
                resteasyReactiveCallbackContext.registerCompletionCallback((t) -> {
                    if (callbackException != null)
                        throw ExceptionUtil.removeStackTrace(new RuntimeException("Callback called twice"));
                    callbackException = Objects.toString(t);
                });
                if ("true".equals(requestContext.getHeaderString("UseExceptionMapper")))
                    requestContext.resume(ExceptionUtil.removeStackTrace(new AsyncFilterException("ouch")));
                else
                    requestContext.resume(ExceptionUtil.removeStackTrace(new Throwable("ouch")));
            });
        }
        LOG.debug("Filter response for " + name + " with action: " + action + " done");
    }

    @SuppressWarnings("unchecked")
    private void addValuesToContext(ContainerResponseContext responseContext, Entry<String, List<String>> entry) {
        // cast required to disambiguate with Object... method
        responseContext.getHeaders().addAll(entry.getKey(), (List) entry.getValue());
    }
}
