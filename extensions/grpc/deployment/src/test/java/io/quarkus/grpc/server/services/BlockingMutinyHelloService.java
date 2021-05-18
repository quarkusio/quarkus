
package io.quarkus.grpc.server.services;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@GrpcService
public class BlockingMutinyHelloService implements Greeter {

    @Override
    @Blocking
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(request.getName())
                .map(s -> Thread.currentThread().getName() + " " + s)
                .map(s -> HelloReply.newBuilder().setMessage(s).build());
    }
}
