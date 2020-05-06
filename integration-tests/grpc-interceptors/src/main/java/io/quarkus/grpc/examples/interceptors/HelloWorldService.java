package io.quarkus.grpc.examples.interceptors;

import javax.inject.Singleton;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.smallrye.mutiny.Uni;

@Singleton
public class HelloWorldService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
