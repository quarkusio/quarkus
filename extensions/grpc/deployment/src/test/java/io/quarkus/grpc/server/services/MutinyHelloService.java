package io.quarkus.grpc.server.services;

import javax.inject.Singleton;

import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.smallrye.mutiny.Uni;

@Singleton
public class MutinyHelloService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(request.getName())
                .map(s -> "Hello " + s)
                .map(s -> HelloReply.newBuilder().setMessage(s).build());
    }
}
