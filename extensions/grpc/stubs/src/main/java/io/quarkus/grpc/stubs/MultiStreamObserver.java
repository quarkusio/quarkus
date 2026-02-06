package io.quarkus.grpc.stubs;

import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiStreamObserver<X, T> extends MutinyClientStreamObserver<X, T> {

    private final MultiEmitter<? super T> emitter;

    public MultiStreamObserver(MultiEmitter<? super T> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onNext(T item) {
        emitter.emit(item);
    }

    @Override
    public void onError(Throwable failure) {
        emitter.fail(failure);
    }

    @Override
    public void onCompleted() {
        emitter.complete();
    }
}
