package io.quarkus.grpc.server.interceptors;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@GrpcService
public class MyInterceptedGreeting implements Greeter {
    @Override
    @Blocking
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom()
                .item(() -> HelloReply.newBuilder()
                        .setMessage("hello " + MyFirstInterceptor.KEY_1.get() + " - " + MyFirstInterceptor.KEY_2.get())
                        .build());
    }

    @Override
    @Blocking
    public Uni<HelloReply> wEIRD(HelloRequest request) {
        return sayHello(request);
    }
}
