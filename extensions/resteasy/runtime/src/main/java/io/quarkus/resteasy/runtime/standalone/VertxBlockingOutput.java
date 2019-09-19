package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public class VertxBlockingOutput implements VertxOutput {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    protected boolean waitingForDrain;
    protected boolean drainHandlerRegistered;
    protected final HttpServerRequest request;

    public VertxBlockingOutput(HttpServerRequest request) {
        this.request = request;
        request.response().exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.debugf(event, "IO Exception ");
                //TODO: do we need this?
                terminateResponse();
                request.connection().close();
            }
        });

        request.response().endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notify();
                    }
                }
                terminateResponse();
            }
        });
    }

    public void terminateResponse() {

    }

    Buffer createBuffer(ByteBuf data) {
        return new VertxBufferImpl(data);
    }

    @Override
    public void write(ByteBuf data, boolean last) throws IOException {
        if (last && data == null) {
            request.response().end();
            return;
        }
        try {
            //do all this in the same lock
            synchronized (request.connection()) {
                awaitWriteable();
                if (last) {
                    request.response().end(createBuffer(data));
                } else {
                    request.response().write(createBuffer(data));
                }
            }
        } finally {
            if (last) {
                terminateResponse();
            }
        }
    }

    private void awaitWriteable() throws IOException {
        assert Thread.holdsLock(request.connection());
        while (request.response().writeQueueFull()) {
            if (Context.isOnEventLoopThread()) {
                throw new IOException("Attempting a blocking write on io thread");
            }
            if (!drainHandlerRegistered) {
                drainHandlerRegistered = true;
                request.response().drainHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        if (waitingForDrain) {
                            request.connection().notifyAll();
                        }
                    }
                });
            }
            try {
                waitingForDrain = true;
                request.connection().wait();
            } catch (InterruptedException e) {
                throw new InterruptedIOException(e.getMessage());
            } finally {
                waitingForDrain = false;
            }
        }
    }

}
