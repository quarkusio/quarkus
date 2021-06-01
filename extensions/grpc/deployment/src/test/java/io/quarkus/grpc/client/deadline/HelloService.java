package io.quarkus.grpc.client.deadline;

import java.time.Duration;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloService implements Greeter {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage("OK").build()).onItem().delayIt()
                .by(Duration.ofMillis(400));
    }

}
