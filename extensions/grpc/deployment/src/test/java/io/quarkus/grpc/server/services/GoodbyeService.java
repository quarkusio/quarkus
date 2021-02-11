package io.quarkus.grpc.server.services;

import javax.inject.Singleton;

import io.grpc.examples.goodbyeworld.FarewellGrpc;
import io.grpc.examples.goodbyeworld.GoodbyeReply;
import io.grpc.examples.goodbyeworld.GoodbyeRequest;
import io.grpc.stub.StreamObserver;

@Singleton
public class GoodbyeService extends FarewellGrpc.FarewellImplBase {

    @Override
    public void sayGoodbye(GoodbyeRequest request, StreamObserver<GoodbyeReply> responseObserver) {
        responseObserver.onNext(GoodbyeReply.newBuilder().setMessage("Goodbye " + request.getName()).build());
        responseObserver.onCompleted();
    }
}
