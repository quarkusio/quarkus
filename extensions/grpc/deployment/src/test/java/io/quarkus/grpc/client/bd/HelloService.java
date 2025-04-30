package io.quarkus.grpc.client.bd;

import java.time.Duration;

import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloService extends GreeterGrpc.GreeterImplBase {

    @Override
    @Blocking
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> observer) {
        Deadline deadline = Context.current().getDeadline();
        if (deadline == null) {
            throw new IllegalStateException("Null deadline");
        }
        Uni.createFrom()
                .item(HelloReply.newBuilder().setMessage("OK").build())
                .onItem()
                .delayIt()
                .by(Duration.ofMillis(400)).invoke(observer::onNext)
                .invoke(observer::onCompleted)
                .await()
                .indefinitely();
    }

}
