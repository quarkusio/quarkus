package io.quarkus.vertx.http.runtime.filters;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.quarkus.vertx.http.runtime.AbstractRequestWrapper;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

class ShutdownRequestWrapper extends AbstractRequestWrapper {

    private volatile int done;
    private static final AtomicIntegerFieldUpdater<ShutdownRequestWrapper> doneUpdater = AtomicIntegerFieldUpdater
            .newUpdater(ShutdownRequestWrapper.class, "done");
    private final Handler<Void> requestDoneHandler;

    private Handler<Throwable> exceptionHandler;
    private final AbstractResponseWrapper response;

    public ShutdownRequestWrapper(HttpServerRequest event, Handler<Void> requestDoneHandler) {
        super(event);
        this.requestDoneHandler = requestDoneHandler;
        this.response = new ResponseWrapper(delegate.response());
        event.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                if (exceptionHandler != null) {
                    exceptionHandler.handle(event);
                }
                done();
            }
        });
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    void done() {
        if (doneUpdater.compareAndSet(this, 0, 1)) {
            requestDoneHandler.handle(null);
        }
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    class ResponseWrapper extends AbstractResponseWrapper {

        Handler<Void> endHandler;
        Handler<Void> closeHandler;
        Handler<Throwable> exceptionHandler;

        ResponseWrapper(HttpServerResponse delegate) {
            super(delegate);
            delegate.closeHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    done();
                    if (closeHandler != null) {
                        closeHandler.handle(event);
                    }
                }
            });
            delegate.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable event) {
                    done();

                    if (exceptionHandler != null) {
                        exceptionHandler.handle(event);
                    }
                }
            });
            delegate.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    done();
                    if (endHandler != null) {
                        endHandler.handle(event);
                    }
                }
            });
        }

        @Override
        public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
            this.exceptionHandler = handler;
            return this;
        }

        @Override
        public HttpServerResponse closeHandler(Handler<Void> handler) {
            this.closeHandler = handler;
            return this;
        }

        @Override
        public HttpServerResponse endHandler(Handler<Void> handler) {
            this.endHandler = handler;
            return this;
        }
    }

}
