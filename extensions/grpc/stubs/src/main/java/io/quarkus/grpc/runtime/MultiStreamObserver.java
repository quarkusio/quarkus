package io.quarkus.grpc.runtime;

import io.grpc.stub.StreamObserver;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class MultiStreamObserver<T> implements StreamObserver<T> {

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
