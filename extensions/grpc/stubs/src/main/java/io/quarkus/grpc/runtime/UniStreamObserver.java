package io.quarkus.grpc.runtime;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniStreamObserver<T> implements StreamObserver<T> {

    private final UniEmitter<? super T> emitter;

    public UniStreamObserver(UniEmitter<? super T> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onNext(T item) {
        emitter.complete(item);
    }

    @Override
    public void onError(Throwable failure) {
        emitter.fail(failure);
    }

    @Override
    public void onCompleted() {
        // Do nothing.
    }
}
