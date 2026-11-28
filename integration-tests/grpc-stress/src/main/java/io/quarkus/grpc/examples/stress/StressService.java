package io.quarkus.grpc.examples.stress;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class StressService extends MutinyStressGrpc.StressImplBase {

    @Override
    public Uni<IdReply> echoId(IdRequest request) {
        return Uni.createFrom().item(IdReply.newBuilder().setId(request.getId()).build());
    }

    @Override
    public Uni<PayloadReply> echoPayload(PayloadRequest request) {
        return Uni.createFrom().item(PayloadReply.newBuilder()
                .setId(request.getId())
                .setSize(request.getData().size())
                .build());
    }

    @Override
    public Multi<IdReply> streamIds(Multi<IdRequest> request) {
        return request.map(r -> IdReply.newBuilder().setId(r.getId()).build());
    }
}
