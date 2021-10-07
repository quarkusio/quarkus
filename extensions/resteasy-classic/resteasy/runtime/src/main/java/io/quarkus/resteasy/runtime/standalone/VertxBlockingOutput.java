package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.vertx.core.runtime.VertxBufferImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;

public class VertxBlockingOutput implements VertxOutput {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    protected boolean waitingForDrain;
    protected boolean drainHandlerRegistered;
    protected final HttpServerRequest request;
    protected boolean first = true;
    protected Throwable throwable;

    public VertxBlockingOutput(HttpServerRequest request) {
        this.request = request;
        request.response().exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                throwable = event;
                log.debugf(event, "IO Exception ");
                //TODO: do we need this?
                terminateResponse();
                request.connection().close();
                synchronized (request.connection()) {
                    if (waitingForDrain) {
                        request.connection().notify();
                    }
                }
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
            //if there is a problem we still try and end, but then throw to report to the caller
            if (throwable != null) {
                throw new IOException(throwable);
            }
            return;
        }
        if (throwable != null) {
            throw new IOException(throwable);
        }
        try {
            //do all this in the same lock
            synchronized (request.connection()) {
                try {
                    awaitWriteable();
                    if (last) {
                        request.response().end(createBuffer(data));
                    } else {
                        request.response().write(createBuffer(data));
                    }
                } catch (Exception e) {
                    if (data != null && data.refCnt() > 0) {
                        data.release();
                    }
                    throw new IOException("Failed to write", e);
                }
            }
        } finally {
            if (last) {
                terminateResponse();
            }
        }
    }

    @Override
    public CompletionStage<Void> writeNonBlocking(ByteBuf data, boolean last) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        if (last && data == null) {
            request.response().end(handler(ret));
            return ret;
        }
        Buffer buffer = createBuffer(data);
        if (last) {
            request.response().end(buffer, handler(ret));
        } else {
            request.response().write(buffer, handler(ret));
        }
        //no need to free 'data', the write will handle this
        return ret;
    }

    private <T extends Throwable> void rethrow(Throwable x) throws T {
        throw (T) x;
    }

    private Handler<AsyncResult<Void>> handler(CompletableFuture<Void> ret) {
        return res -> {
            if (res.succeeded())
                ret.complete(null);
            else
                ret.completeExceptionally(res.cause());
        };
    }

    private void awaitWriteable() throws IOException {
        if (first) {
            first = false;
            return;
        }
        assert Thread.holdsLock(request.connection());
        while (request.response().writeQueueFull()) {
            if (throwable != null) {
                throw new IOException(throwable);
            }
            if (Context.isOnEventLoopThread()) {
                throw new BlockingOperationNotAllowedException("Attempting a blocking write on io thread");
            }
            if (request.response().closed()) {
                throw new IOException("Connection has been closed");
            }
            if (!drainHandlerRegistered) {
                drainHandlerRegistered = true;
                Handler<Void> handler = new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        if (waitingForDrain) {
                            HttpConnection connection = request.connection();
                            synchronized (connection) {
                                connection.notifyAll();
                            }
                        }
                    }
                };
                request.response().drainHandler(handler);
                request.response().closeHandler(handler);
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
