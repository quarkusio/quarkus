package io.quarkus.it.pulsar;

import examples.GpReply;
import examples.GpRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class GpServiceImpl extends MutinyGreeterGrpc.GreeterImplBase {
    @Override
    public Uni<GpReply> greet(GpRequest request) {
        return Uni.createFrom().item(GpReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }
}
