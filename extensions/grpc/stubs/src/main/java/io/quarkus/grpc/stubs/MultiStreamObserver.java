package io.quarkus.grpc.stubs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiStreamObserver<I, O> implements ClientResponseObserver<I, O> {
    private final MultiEmitter<? super O> emitter;

    // If this is set, the Multi was terminated by completion or failure, not by cancellation.
    private final AtomicBoolean terminated = new AtomicBoolean();

    private final AtomicReference<ClientCallStreamObserver<I>> requestStreamObserver = new AtomicReference<>();
    private final Runnable onReady;
    private ClientCallStreamObserver<I> requestStream;
    private long pending = 0;

    public MultiStreamObserver(MultiEmitter<? super O> emitter, Runnable onTermination, Runnable onReady) {
        this.onReady = onReady;
        this.emitter = emitter.onTermination(() -> {
            // This is called when the reply Multi (the return value of the RPC method) is cancelled or the gRPC server completes the request.
            if (terminated.compareAndSet(false, true)) {
                // Cancel the gRPC stream on reply Multi cancellation only (not on completion, this would unnecessarily reset the HTTP/2 stream)
                ClientCallStreamObserver<I> observer = requestStreamObserver.getAndSet(null);
                if (observer != null) {
                    observer.cancel("Cancelled by Multi!", null);
                }
            }

            if (onTermination != null) {
                onTermination.run();
            }
        });
    }

    public MultiStreamObserver(MultiEmitter<? super O> emitter, Runnable onTermination) {
        this(emitter, onTermination, null);
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<I> requestStreamObserver) {
        this.requestStreamObserver.set(requestStreamObserver);
        this.requestStream = requestStreamObserver;

        long requested = emitter.requested();
        // if requested == Long.MAX_VALUE we leave autoRequest on
        if (requested < Long.MAX_VALUE && requested > Integer.MAX_VALUE) { // if it is < Long.MAX_VALUE we disable it and request a lot
            this.requestStream.disableAutoRequestWithInitial(Integer.MAX_VALUE);
            pending = requested - Integer.MAX_VALUE;
        } else if (requested < Integer.MAX_VALUE) {
            this.requestStream.disableAutoRequestWithInitial((int) requested);
        }

        this.emitter.onRequest(demand -> {
            if (demand > Integer.MAX_VALUE) {
                this.requestStream.request(Integer.MAX_VALUE);
                pending += Integer.MAX_VALUE;
            } else if (demand > 0) {
                this.requestStream.request((int) demand);
            }
        });

        if (onReady != null) {
            requestStreamObserver.setOnReadyHandler(onReady);
        }
    }

    @Override
    public void onNext(O item) {
        if (requestStream != null) {
            if (pending > Integer.MAX_VALUE) {
                requestStream.request(Integer.MAX_VALUE);
                pending -= Integer.MAX_VALUE;
            } else if (pending > 0) {
                requestStream.request((int) pending);
                pending = 0;
            }
        }

        emitter.emit(item);
    }

    @Override
    public void onError(Throwable failure) {
        if (terminated.compareAndSet(false, true)) {
            emitter.fail(failure);
        } else {
            // If the reply Multi is cancelled, the ClientCallStreamObserver will be cancelled (see constructor).
            // ClientCallStreamObserver.cancel causes onError to be called with an io.grpc.StatusRuntimeException.
            // We are not interested in this exception and we do not want the emitter to fail because this exception
            // cannot be handled by Mutiny because the Multi has already been terminated.
        }
    }

    @Override
    public void onCompleted() {
        terminated.set(true);
        emitter.complete();
    }
}
