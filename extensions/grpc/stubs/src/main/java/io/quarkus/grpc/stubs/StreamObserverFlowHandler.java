package io.quarkus.grpc.stubs;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;

public class StreamObserverFlowHandler<T> implements StreamObserver<T> {

    private final Object waitLock = new Object();
    private final CallStreamObserver<T> wrapped;

    public StreamObserverFlowHandler(StreamObserver<T> wrapped) {
        if (!(wrapped instanceof CallStreamObserver<?>)) {
            throw new IllegalStateException("Wrapped StreamObserver must be instance of CallStreamObserver!");
        }
        this.wrapped = (CallStreamObserver<T>) wrapped;
    }

    @Override
    public void onNext(T value) {
        while (!wrapped.isReady()) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(0, 100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }
        wrapped.onNext(value);
    }

    @Override
    public void onError(Throwable t) {
        wrapped.onError(t);
    }

    @Override
    public void onCompleted() {
        wrapped.onCompleted();
    }
}
