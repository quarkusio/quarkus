package io.quarkus.grpc.examples.hello;

import com.google.protobuf.Empty;

import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldNewService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }

    @Override
    public Uni<HelloReply> sayJo(Empty request) {
        return Uni.createFrom().item("Jo!")
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }

    @Override
    public Uni<HelloReply> threadName(HelloRequest request) {
        return Uni.createFrom().item(Thread.currentThread().getName())
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
