package org.jboss.resteasy.reactive.server.handlers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.AbstractCancellableServerRestHandler;

public class CompletionStageResponseHandler extends AbstractCancellableServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a CompletionStage
        if (requestContext.getResult() instanceof CompletionStage<?> result) {
            requestContext.suspend();

            AtomicBoolean done = new AtomicBoolean();
            AtomicBoolean canceled = new AtomicBoolean();
            result.handle((v, t) -> {
                done.set(true);
                if (canceled.get()) {
                    try {
                        // get rid of everything related to the request since the connection has already gone away
                        requestContext.close();
                    } catch (Exception ignored) {

                    }
                } else {
                    if (t != null) {
                        if (t instanceof CompletionException ce) {
                            requestContext.handleException(ce.getCause(), true);
                        } else {
                            requestContext.handleException(t, true);
                        }
                    } else {
                        requestContext.setResult(v);
                    }
                    requestContext.resume();
                }
                return null;
            });

            requestContext.serverResponse().addCloseHandler(new Runnable() {
                @Override
                public void run() {
                    if (isCancellable() && !done.get()) {
                        if (result instanceof CompletableFuture<?> cf) {
                            canceled.set(true);
                            cf.cancel(true);
                        }
                    }
                }
            });
        }
    }
}
