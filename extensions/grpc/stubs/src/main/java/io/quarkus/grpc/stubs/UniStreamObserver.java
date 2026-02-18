package io.quarkus.grpc.stubs;

import io.smallrye.mutiny.subscription.UniEmitter;

public class UniStreamObserver<X, T> extends MutinyClientStreamObserver<X, T> {

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
