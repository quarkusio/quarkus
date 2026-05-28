package io.quarkus.grpc.external.proto;

import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.external.bareproto.BareProtoMessage;
import io.quarkus.grpc.external.bareproto.BareProtoService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class BareProtoGrpcService implements BareProtoService {

    @Override
    public Uni<BareProtoMessage> send(BareProtoMessage request) {
        return Uni.createFrom().item(
                BareProtoMessage.newBuilder().setContent("reply:" + request.getContent()).build());
    }
}
