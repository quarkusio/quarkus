package io.quarkus.grpc.runtime.devmode;

import java.util.HashSet;
import java.util.Set;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.runtime.StreamCollector;

public class DevModeStreamsCollector implements StreamCollector {
    private final Set<StreamObserver<?>> streamObservers = new HashSet<>();

    @Override
    public <O> void add(StreamObserver<O> observer) {
        streamObservers.add(observer);
    }

    @Override
    public <O> void remove(StreamObserver<O> observer) {
        streamObservers.remove(observer);
    }

    public void shutdown() {
        streamObservers.forEach(this::complete);
    }

    private void complete(StreamObserver<?> streamObserver) {
        try {
            streamObserver.onCompleted();
        } catch (Exception ignored) {
        }
    }
}
