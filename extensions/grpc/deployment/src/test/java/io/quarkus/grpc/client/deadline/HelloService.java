package io.quarkus.grpc.client.deadline;

import java.time.Duration;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloService implements Greeter {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        Deadline deadline = Context.current().getDeadline();
        if (deadline == null) {
            throw new IllegalStateException("Null deadline");
        }
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage("OK").build()).onItem().delayIt()
                .by(Duration.ofMillis(400));
    }

    @Override
    public Uni<HelloReply> wEIRD(HelloRequest request) {
        return sayHello(request);
    }

}
