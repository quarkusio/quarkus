package io.quarkus.grpc.client.deadline.percall;

import java.time.Duration;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class SlowHelloService implements Greeter {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        Deadline deadline = Context.current().getDeadline();
        if (deadline == null) {
            throw new IllegalStateException("Null deadline");
        }
        Uni<HelloReply> reply = Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
        if ("slow".equals(request.getName())) {
            return reply.onItem().delayIt().by(Duration.ofMillis(600));
        }
        return reply;
    }

    @Override
    public Uni<HelloReply> wEIRD(HelloRequest request) {
        return sayHello(request);
    }
}
