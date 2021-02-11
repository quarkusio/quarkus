package io.quarkus.grpc.protov2;

import javax.inject.Singleton;

import io.grpc.examples.helloworld.v2.HelloReply;
import io.grpc.examples.helloworld.v2.HelloRequest;
import io.grpc.examples.helloworld.v2.MutinyGreeterGrpc;
import io.smallrye.mutiny.Uni;

@Singleton
public class HelloServiceV2 extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item(name).map(s -> HelloReply.newBuilder().setMessage("hello " + name).build());
    }
}
