package io.quarkus.grpc;

import io.grpc.stub.AbstractStub;

/**
 * Represents a convenient Mutiny client generated for a gRPC service.
 */
public interface MutinyClient<T extends AbstractStub<T>> {

    /**
     *
     * @return the underlying stub
     */
    T getStub();

    MutinyClient<T> newInstanceWithStub(T stub);
}
