package io.quarkus.it.grpc.uds;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item(
                HelloReply.newBuilder().setMessage("Hello " + name).build());
    }
}
