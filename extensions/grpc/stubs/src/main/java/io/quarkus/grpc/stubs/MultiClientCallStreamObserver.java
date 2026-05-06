package io.quarkus.grpc.stubs;

import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiClientCallStreamObserver<I, O> implements ClientResponseObserver<I, O>, StreamObserver<O> {

    private final MultiEmitter<? super O> emitter;
    private ClientCallStreamObserver<I> requestStream;
    private long pending = 0;

    MultiClientCallStreamObserver(MultiEmitter<? super O> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<I> requestStream) {
        this.requestStream = requestStream;
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
    }

    @Override
    public void onNext(O value) {
        if (pending > Integer.MAX_VALUE) {
            this.requestStream.request(Integer.MAX_VALUE);
            pending -= Integer.MAX_VALUE;
        } else if (pending > 0) {
            this.requestStream.request((int) pending);
            pending = 0;
        }

        emitter.emit(value);
    }

    @Override
    public void onError(Throwable t) {
        emitter.fail(t);
    }

    @Override
    public void onCompleted() {
        emitter.complete();
    }
}
