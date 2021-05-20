package io.quarkus.grpc.server.services;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class MutinyHelloService implements Greeter {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(request.getName())
                .map(s -> "Hello " + s)
                .map(s -> HelloReply.newBuilder().setMessage(s).build());
    }
}
