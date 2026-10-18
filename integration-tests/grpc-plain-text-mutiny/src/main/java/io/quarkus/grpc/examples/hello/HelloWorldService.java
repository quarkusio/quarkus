package io.quarkus.grpc.examples.hello;

import java.util.concurrent.atomic.AtomicInteger;

import examples.HelloReply;
import examples.HelloRequest;
import examples.LanguageSpec;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.smallrye.mutiny.Uni;

@GrpcService
@RegisterInterceptor(IncomingInterceptor.class)
public class HelloWorldService extends MutinyGreeterGrpc.GreeterImplBase {

    AtomicInteger counter = new AtomicInteger();

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        int count = counter.incrementAndGet();
        String name = request.getName();
        return Uni.createFrom().item("Hello " + name)
                .map(res -> HelloReply.newBuilder().setMessage(res).setCount(count).build());
    }

    @Override
    public Uni<HelloReply> greeting(LanguageSpec request) {
        return Uni.createFrom().item(() -> {
            String res = null;
            switch (request.getSelectedLanguage()) {
                case FRENCH:
                    res = "Bonjour!";
                    break;
                case SPANISH:
                    res = "Hola!";
                    break;
                case ENGLISH:
                    res = "Hello!";
                    break;
                case UNRECOGNIZED:
                    res = "Blurp!";
                    break;
            }
            return res;
        })
                .map(res -> HelloReply.newBuilder().setMessage(res).build());
    }
}
