package org.jboss.resteasy.reactive.client.impl.multipart;

import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * Based on {@link io.vertx.core.streams.impl.PipeImpl}.
 * We can't use the aforementioned class as it is not open enough for us to add the read tracking we want.
 */
class RequestTrackingPipe<T> implements Pipe<T> {

    private final long maxInFlightReads;

    private final Promise<Void> result;
    private final ReadStream<T> src;
    private boolean endOnSuccess = true;
    private boolean endOnFailure = true;
    private WriteStream<T> dst;

    private boolean manuallyPaused = false;

    private final AtomicLong inFlightReads = new AtomicLong(0);

    public RequestTrackingPipe(ReadStream<T> src, long maxInFlightReads) {
        this.src = src;
        this.maxInFlightReads = maxInFlightReads;
        this.result = Promise.promise();

        // Set handlers now
        src.endHandler(result::tryComplete);
        src.exceptionHandler(result::tryFail);
    }

    @Override
    public synchronized Pipe<T> endOnFailure(boolean end) {
        endOnFailure = end;
        return this;
    }

    @Override
    public synchronized Pipe<T> endOnSuccess(boolean end) {
        endOnSuccess = end;
        return this;
    }

    @Override
    public synchronized Pipe<T> endOnComplete(boolean end) {
        endOnSuccess = end;
        endOnFailure = end;
        return this;
    }

    private void handleWriteResult(AsyncResult<Void> ack) {
        long currentInFlightReads = inFlightReads.decrementAndGet();
        if (currentInFlightReads <= (maxInFlightReads / 2)) {
            synchronized (RequestTrackingPipe.this) {
                if (manuallyPaused) {
                    manuallyPaused = false;
                    src.resume();
                }
            }
        }
        if (ack.failed()) {
            result.tryFail(new WriteException(ack.cause()));
        }
    }

    @Override
    public void to(WriteStream<T> ws, Handler<AsyncResult<Void>> completionHandler) {
        if (ws == null) {
            throw new NullPointerException();
        }
        boolean endOnSuccess;
        boolean endOnFailure;
        synchronized (RequestTrackingPipe.this) {
            if (dst != null) {
                throw new IllegalStateException();
            }
            dst = ws;
            endOnSuccess = this.endOnSuccess;
            endOnFailure = this.endOnFailure;
        }
        Handler<Void> drainHandler = v -> src.resume();
        src.handler(item -> {
            ws.write(item, this::handleWriteResult);
            long currentInFlightReads = inFlightReads.incrementAndGet();
            if (ws.writeQueueFull()) {
                src.pause();
                ws.drainHandler(drainHandler);
            } else if (currentInFlightReads > maxInFlightReads) {
                synchronized (RequestTrackingPipe.this) {
                    src.pause();
                    manuallyPaused = true;
                }
            }
        });
        src.resume();
        result.future().onComplete(ar -> {
            try {
                src.handler(null);
            } catch (Exception ignore) {
            }
            try {
                src.exceptionHandler(null);
            } catch (Exception ignore) {
            }
            try {
                src.endHandler(null);
            } catch (Exception ignore) {
            }
            if (ar.succeeded()) {
                handleSuccess(completionHandler);
            } else {
                Throwable err = ar.cause();
                if (err instanceof WriteException) {
                    src.resume();
                    err = err.getCause();
                }
                handleFailure(err, completionHandler);
            }
        });
    }

    private void handleSuccess(Handler<AsyncResult<Void>> completionHandler) {
        if (endOnSuccess) {
            dst.end(completionHandler);
        } else {
            completionHandler.handle(Future.succeededFuture());
        }
    }

    private void handleFailure(Throwable cause, Handler<AsyncResult<Void>> completionHandler) {
        Future<Void> res = Future.failedFuture(cause);
        if (endOnFailure) {
            dst.end(ignore -> {
                completionHandler.handle(res);
            });
        } else {
            completionHandler.handle(res);
        }
    }

    public void close() {
        synchronized (this) {
            src.exceptionHandler(null);
            src.handler(null);
            if (dst != null) {
                dst.drainHandler(null);
                dst.exceptionHandler(null);
            }
        }
        VertxException err = new VertxException("Pipe closed", true);
        if (result.tryFail(err)) {
            src.resume();
        }
    }

    private static class WriteException extends VertxException {
        private WriteException(Throwable cause) {
            super(cause, true);
        }
    }

}
