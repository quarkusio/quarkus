package io.quarkus.it.pulsar;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyHelloGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloServiceImpl extends MutinyHelloGrpc.HelloImplBase {
    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hello World, " + request.getName()).build());
    }
}
