package org.jboss.resteasy.reactive.client.handlers;

import java.util.concurrent.Callable;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.BlockingOperationSupport;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class ClientResponseFilterRestHandler implements ClientRestHandler {

    private final ClientResponseFilter filter;
    private final boolean preserveThread;

    public ClientResponseFilterRestHandler(ClientResponseFilter filter, boolean preserveThread) {
        this.filter = filter;
        this.preserveThread = preserveThread;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        if (preserveThread) {
            doHandle(requestContext);
            return;
        }

        if (BlockingOperationSupport.isBlockingAllowed()) {
            doHandle(requestContext);
        } else {
            ClientRequestContextImpl clientRequestContext = requestContext.getClientRequestContext();
            Context vertxContext = clientRequestContext.getContext();
            if (vertxContext != null) {
                requestContext.suspend();
                vertxContext.executeBlocking(new Callable<>() {
                    @Override
                    public Void call() {
                        doHandle(requestContext);
                        return null;
                    }
                }, false).onComplete(new Handler<>() {
                    @Override
                    public void handle(AsyncResult<Object> event) {
                        if (event.failed()) {
                            vertxContext.runOnContext(new Handler<>() {
                                @Override
                                public void handle(Void event2) {
                                    requestContext.resume(event.cause());
                                }
                            });
                        } else {
                            vertxContext.runOnContext(new Handler<>() {
                                @Override
                                public void handle(Void event2) {
                                    requestContext.resume();
                                }
                            });
                        }
                    }
                });
            } else { // shouldn't ever happen
                doHandle(requestContext);
            }
        }
    }

    private void doHandle(RestClientRequestContext requestContext) {
        try {
            filter.filter(requestContext.getOrCreateClientRequestContext(),
                    requestContext.getOrCreateClientResponseContext());
        } catch (WebApplicationException | ProcessingException x) {
            throw x;
        } catch (Exception x) {
            throw new ProcessingException(x);
        }
    }
}
