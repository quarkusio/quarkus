package org.acme;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import org.acme.protocol.GameService;
import org.acme.protocol.HelloRequest;
import org.acme.protocol.HelloResponse;

@GrpcService
public class GameServiceImpl implements GameService {

    @Override
    public Uni<HelloResponse> hello(HelloRequest request) {
        return Uni.createFrom().item(HelloResponse.newBuilder().setMessage("Hello from gRPC service!").build());
    }
}
