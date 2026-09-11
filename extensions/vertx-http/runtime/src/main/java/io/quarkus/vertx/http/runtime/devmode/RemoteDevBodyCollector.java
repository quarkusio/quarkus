package io.quarkus.vertx.http.runtime.devmode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

final class RemoteDevBodyCollector {

    private final RemoteSyncHandler owner;
    private final HttpServerRequest request;
    private final RemoteDevBodyLimits limits;
    private final RemoteDevBodyAdmission admission;
    private final RemoteDevBodySpoolStore spoolStore;
    private final Consumer<CompletedBody> completion;
    private final long declaredLength;

    private State state = State.NEW;
    private RemoteDevBodyAdmission.Reservation reservation;
    private RemoteDevBodySpoolStore.Spool spool;
    private long received;
    private boolean writePending;

    private RemoteDevBodyCollector(RemoteSyncHandler owner, HttpServerRequest request, RemoteDevBodyLimits limits,
            RemoteDevBodyAdmission admission, RemoteDevBodySpoolStore spoolStore, long declaredLength,
            Consumer<CompletedBody> completion) {
        this.owner = owner;
        this.request = request;
        this.limits = limits;
        this.admission = admission;
        this.spoolStore = spoolStore;
        this.declaredLength = declaredLength;
        this.completion = completion;
    }

    static void start(RemoteSyncHandler owner, HttpServerRequest request, RemoteDevBodyLimits limits,
            RemoteDevBodyAdmission admission, RemoteDevBodySpoolStore spoolStore, Consumer<CompletedBody> completion) {
        request.pause();
        String encoding = request.getHeader(HttpHeaderNames.CONTENT_ENCODING);
        if (encoding != null && !"identity".equalsIgnoreCase(encoding.trim())) {
            owner.rejectBody(request, 415, "Remote dev does not support encoded request bodies");
            return;
        }
        long declaredLength;
        try {
            declaredLength = contentLength(request.getHeader(HttpHeaderNames.CONTENT_LENGTH));
        } catch (IllegalArgumentException e) {
            owner.rejectBody(request, 400, "Remote dev request has an invalid Content-Length");
            return;
        }
        if (declaredLength > limits.requestLimit()) {
            owner.rejectBody(request, 413,
                    "Remote dev request body exceeds quarkus.http.limits.max-body-size");
            return;
        }
        var collector = new RemoteDevBodyCollector(owner, request, limits, admission, spoolStore, declaredLength,
                Objects.requireNonNull(completion));
        collector.begin();
    }

    private static long contentLength(String header) {
        if (header == null) {
            return -1;
        }
        try {
            long length = Long.parseLong(header);
            if (length < 0) {
                throw new IllegalArgumentException("negative Content-Length");
            }
            return length;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid Content-Length", e);
        }
    }

    private void begin() {
        long initialReservation = declaredLength >= 0 ? declaredLength : 0;
        reservation = admission.tryAcquire(initialReservation);
        if (reservation == null) {
            owner.rejectBody(request, 503, "Remote dev request-body capacity is temporarily unavailable");
            return;
        }
        synchronized (this) {
            state = State.RECEIVING;
        }
        owner.collectorStarted(this);
        request.handler(this::receive)
                .endHandler(ignored -> end())
                .exceptionHandler(this::fail);
        request.response().closeHandler(ignored -> cancel());
        spoolStore.create().onComplete(result -> {
            if (result.failed()) {
                fail(result.cause());
                return;
            }
            boolean resume;
            synchronized (this) {
                spool = result.result();
                resume = state == State.RECEIVING;
            }
            if (resume) {
                request.resume();
            } else {
                cleanupSpool(result.result());
            }
        });
    }

    private void receive(Buffer buffer) {
        byte[] bytes = buffer.getBytes();
        synchronized (this) {
            if (state != State.RECEIVING || writePending) {
                return;
            }
            if (bytes.length > limits.requestLimit() - received) {
                reject(413, "Remote dev request body exceeds quarkus.http.limits.max-body-size");
                return;
            }
            if (declaredLength >= 0 && bytes.length > declaredLength - received) {
                reject(400, "Remote dev request body exceeds its Content-Length");
                return;
            }
            if (declaredLength < 0 && !admission.tryReserve(reservation, bytes.length)) {
                reject(503, "Remote dev request-body capacity is temporarily unavailable");
                return;
            }
            received += bytes.length;
            writePending = true;
            request.pause();
        }
        executeBlocking(() -> {
            spool.write(bytes);
            return null;
        }).onComplete(result -> {
            boolean resume = false;
            synchronized (this) {
                writePending = false;
                if (result.succeeded() && state == State.RECEIVING) {
                    resume = true;
                }
            }
            if (result.failed()) {
                fail(result.cause());
            } else if (resume) {
                request.resume();
            }
        });
    }

    private void end() {
        synchronized (this) {
            if (state != State.RECEIVING) {
                return;
            }
            if (writePending) {
                fail(new IOException("Remote-dev request ended while a spool write was pending"));
                return;
            }
            if (declaredLength >= 0 && received != declaredLength) {
                reject(400, "Remote dev request body does not match its Content-Length");
                return;
            }
            state = State.CLOSING;
        }
        executeBlocking(() -> {
            spool.closeForReading();
            return null;
        }).onComplete(result -> {
            if (result.failed()) {
                fail(result.cause());
                return;
            }
            CompletedBody body;
            synchronized (this) {
                if (state != State.CLOSING) {
                    return;
                }
                state = State.COMPLETE;
                body = new CompletedBody(this, spool, received);
            }
            try {
                completion.accept(body);
            } catch (Throwable t) {
                body.close();
                owner.bodyProcessingFailed(request, t);
            }
        });
    }

    private void reject(int status, String message) {
        if (!terminate(State.REJECTED)) {
            return;
        }
        owner.rejectBody(request, status, message);
        cleanup();
    }

    private void fail(Throwable failure) {
        if (!terminate(State.FAILED)) {
            return;
        }
        owner.bodyCollectionFailed(request, failure);
        cleanup();
    }

    void cancel() {
        if (!terminate(State.CANCELLED)) {
            return;
        }
        cleanup();
    }

    private synchronized boolean terminate(State terminalState) {
        if (state == State.REJECTED || state == State.FAILED || state == State.CANCELLED) {
            return false;
        }
        if (state == State.COMPLETE) {
            return false;
        }
        state = terminalState;
        request.pause();
        return true;
    }

    private void cleanup() {
        RemoteDevBodySpoolStore.Spool currentSpool;
        synchronized (this) {
            currentSpool = spool;
        }
        if (currentSpool == null) {
            release();
        } else {
            cleanupSpool(currentSpool);
        }
    }

    private void cleanupSpool(RemoteDevBodySpoolStore.Spool currentSpool) {
        spoolStore.delete(currentSpool).onComplete(result -> {
            if (result.failed()) {
                owner.bodyCleanupFailed();
            }
            release();
        });
    }

    private void release() {
        admission.release(reservation);
        owner.collectorClosed(this);
    }

    private void completeBody(RemoteDevBodySpoolStore.Spool completedSpool) {
        cleanupSpool(completedSpool);
    }

    private <T> Future<T> executeBlocking(Callable<T> action) {
        try {
            return owner.executeBlocking(action);
        } catch (RejectedExecutionException e) {
            return Future.failedFuture(e);
        }
    }

    enum State {
        NEW,
        RECEIVING,
        CLOSING,
        COMPLETE,
        REJECTED,
        FAILED,
        CANCELLED
    }

    static final class CompletedBody implements AutoCloseable {

        private final RemoteDevBodyCollector collector;
        private final RemoteDevBodySpoolStore.Spool spool;
        private final long length;
        private final AtomicBoolean closed = new AtomicBoolean();

        private CompletedBody(RemoteDevBodyCollector collector, RemoteDevBodySpoolStore.Spool spool, long length) {
            this.collector = collector;
            this.spool = spool;
            this.length = length;
        }

        long length() {
            return length;
        }

        InputStream openInputStream() throws IOException {
            return spool.openInputStream();
        }

        byte[] readAllBytes() throws IOException {
            return spool.readAllBytes();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                collector.completeBody(spool);
            }
        }
    }
}
