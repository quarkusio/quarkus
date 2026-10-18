package io.quarkus.grpc.examples.interceptors;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.grpc.runtime.supports.context.RoutingContextGrpcInterceptor;
import io.vertx.ext.web.RoutingContext;

@GrpcService
@RegisterInterceptor(RoutingContextGrpcInterceptor.class)
public class HelloWorldService extends GreeterGrpc.GreeterImplBase {

    @Inject
    RoutingContext context;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    private HelloReply getReply(HelloRequest request) {
        // just poke for Vert.x based gRPC server-side
        if ("vertx".equals(profile) || "o2n".equals(profile)) {
            // just poke context, so we know it works
            context.request().getHeader("foobar");
        }

        String name = request.getName();
        if (name.equals("Fail")) {
            throw new HelloException(name);
        }
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
