package io.quarkus.grpc.runtime.devmode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.runtime.StreamCollector;

public class DevModeStreamsCollector implements StreamCollector {
    private final Set<StreamObserver<?>> streamObservers = new HashSet<>();

    @Override
    public <O> void add(StreamObserver<O> observer) {
        synchronized (this) {
            streamObservers.add(observer);
        }
    }

    @Override
    public <O> void remove(StreamObserver<O> observer) {
        synchronized (this) {
            streamObservers.remove(observer);
        }
    }

    public void shutdown() {
        List<StreamObserver<?>> observers;
        synchronized (this) {
            observers = new ArrayList<>(streamObservers);
        }
        observers.forEach(this::complete);
    }

    private void complete(StreamObserver<?> streamObserver) {
        try {
            streamObserver.onCompleted();
        } catch (Exception ignored) {
        }
    }
}
