package io.quarkus.grpc.examples.interceptors;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
