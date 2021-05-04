package io.quarkus.grpc.client.tls;

import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldTlsService extends MutinyGreeterGrpc.GreeterImplBase {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
