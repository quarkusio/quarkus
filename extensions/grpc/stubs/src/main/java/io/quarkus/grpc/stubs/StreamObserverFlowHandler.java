package io.quarkus.grpc.stubs;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ArrayBlockingQueue;

public class StreamObserverFlowHandler<T> implements StreamObserver<T> {

    private final CallStreamObserver<T> wrapped;
    private final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(100);

    public StreamObserverFlowHandler(StreamObserver<T> wrapped) {
        if (!(wrapped instanceof CallStreamObserver<?>)) {
            throw new IllegalStateException("Wrapped StreamObserver must be instance of CallStreamObserver!");
        }
        this.wrapped = (CallStreamObserver<T>) wrapped;
    }

    @Override
    public void onNext(T value) {
        if (!wrapped.isReady()) {
            if (queue.offer(value)) {
                return;
            } else {
                while (!wrapped.isReady()) {
                    synchronized (queue) {
                        try {
                            queue.wait(0, 100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            wrapped.onNext(queue.poll());
        }
        wrapped.onNext(value);
    }

    @Override
    public void onError(Throwable t) {
        wrapped.onError(t);
    }

    @Override
    public void onCompleted() {
        while (!queue.isEmpty()) {
            wrapped.onNext(queue.poll());
        }
        wrapped.onCompleted();
    }
}
