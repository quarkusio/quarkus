package io.quarkus.grpc.external.proto;

import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.external.nested.NestedMessage;
import io.quarkus.grpc.external.nested.NestedService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class NestedGrpcService implements NestedService {

    @Override
    public Uni<NestedMessage> send(NestedMessage request) {
        return Uni.createFrom().item(
                NestedMessage.newBuilder().setContent("reply:" + request.getContent()).build());
    }
}
