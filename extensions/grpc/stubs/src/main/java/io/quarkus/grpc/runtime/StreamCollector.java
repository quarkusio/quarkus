package io.quarkus.grpc.runtime;

import io.grpc.stub.StreamObserver;

public interface StreamCollector {
    StreamCollector NO_OP = new StreamCollector() {
        @Override
        public <O> void add(StreamObserver<O> response) {
        }

        @Override
        public <O> void remove(StreamObserver<O> response) {
        }
    };

    <O> void add(StreamObserver<O> response);

    <O> void remove(StreamObserver<O> response);
}
