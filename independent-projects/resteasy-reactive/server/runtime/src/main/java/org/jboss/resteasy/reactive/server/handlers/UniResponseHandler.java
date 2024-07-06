package org.jboss.resteasy.reactive.server.handlers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Uni
        if (requestContext.getResult() instanceof Uni<?> result) {
            requestContext.suspend();

            AtomicBoolean done = new AtomicBoolean();
            Cancellable cancellable = result.subscribe().with(new Consumer<Object>() {
                @Override
                public void accept(Object v) {
                    done.set(true);
                    requestContext.setResult(v);
                    requestContext.resume();
                }
            }, new Consumer<>() {
                @Override
                public void accept(Throwable t) {
                    done.set(true);
                    requestContext.resume(t, true);
                }
            });

            requestContext.serverResponse().addCloseHandler(new Runnable() {
                @Override
                public void run() {
                    if (!done.get()) {
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
