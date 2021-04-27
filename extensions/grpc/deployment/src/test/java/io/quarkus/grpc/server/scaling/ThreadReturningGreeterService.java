package io.quarkus.grpc.server.scaling;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class ThreadReturningGreeterService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String threadName = Thread.currentThread().getName();
        responseObserver.onNext(HelloReply.newBuilder().setMessage(threadName).build());
        responseObserver.onCompleted();
    }
}
