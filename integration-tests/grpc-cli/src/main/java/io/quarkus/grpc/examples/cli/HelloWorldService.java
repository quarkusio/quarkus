package io.quarkus.grpc.examples.cli;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class HelloWorldService extends GreeterGrpc.GreeterImplBase {

    private HelloReply getReply(HelloRequest request) {
        String name = request.getName();
        return HelloReply.newBuilder().setMessage("Hello " + name).build();
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        responseObserver.onNext(getReply(request));
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<HelloRequest> multiHello(StreamObserver<HelloReply> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(HelloRequest helloRequest) {
                responseObserver.onNext(getReply(helloRequest));
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
