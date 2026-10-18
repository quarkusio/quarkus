package io.quarkus.grpc.examples.hello;

import java.util.concurrent.atomic.AtomicInteger;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class HelloWorldService extends GreeterGrpc.GreeterImplBase {

    AtomicInteger counter = new AtomicInteger();

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        int count = counter.incrementAndGet();
        String name = request.getName();
        String res = "Hello " + name;
        responseObserver.onNext(HelloReply.newBuilder().setMessage(res).setCount(count).build());
        responseObserver.onCompleted();
    }
}
