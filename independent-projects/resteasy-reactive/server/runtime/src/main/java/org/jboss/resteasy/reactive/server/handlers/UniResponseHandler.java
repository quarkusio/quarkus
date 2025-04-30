package org.jboss.resteasy.reactive.server.handlers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.AbstractCancellableServerRestHandler;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniResponseHandler extends AbstractCancellableServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Uni
        if (requestContext.getResult() instanceof Uni<?> result) {
            requestContext.suspend();

            AtomicBoolean done = new AtomicBoolean(false);
            Cancellable cancellable = result.subscribe().with(new Consumer<Object>() {
                @Override
                public void accept(Object v) {
                    if (done.compareAndSet(false, true)) {
                        requestContext.setResult(v);
                        requestContext.resume();
                    }
                }
            }, new Consumer<>() {
                @Override
                public void accept(Throwable t) {
                    if (done.compareAndSet(false, true)) {
                        requestContext.resume(t, true);
                    }
                }
            });

            requestContext.serverResponse().addCloseHandler(new Runnable() {
                @Override
                public void run() {
                    if (isCancellable() && done.compareAndSet(false, true)) {
                        cancellable.cancel();
                        try {
                            // get rid of everything related to the request since the connection has already gone away
                            requestContext.close();
                        } catch (Exception ignored) {

                        }
                    }
                }
            });
        }
    }
}
