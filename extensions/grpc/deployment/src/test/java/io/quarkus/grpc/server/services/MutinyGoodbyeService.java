package io.quarkus.grpc.server.services;

import io.grpc.examples.goodbyeworld.GoodbyeReply;
import io.grpc.examples.goodbyeworld.GoodbyeRequest;
import io.grpc.examples.goodbyeworld.MutinyFarewellGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class MutinyGoodbyeService extends MutinyFarewellGrpc.FarewellImplBase {

    @Override
    public Uni<GoodbyeReply> sayGoodbye(GoodbyeRequest request) {
        return Uni.createFrom().item(request.getName())
                .map(s -> "Goodbye " + s)
                .map(s -> GoodbyeReply.newBuilder().setMessage(s).build());
    }
}
