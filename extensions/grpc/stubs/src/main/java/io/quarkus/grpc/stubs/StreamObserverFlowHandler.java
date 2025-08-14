package io.quarkus.grpc.stubs;

import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.LockSupport;

public class StreamObserverFlowHandler<T> implements StreamObserver<T> {

    private final CallStreamObserver<T> wrapped;
    private final Queue<T> queue = new ArrayBlockingQueue<>(100);

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
                    LockSupport.parkNanos(100);
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
