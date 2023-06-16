package ilove.quark.us;

import io.quarkus.grpc.GrpcService;

import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloGrpcService implements HelloGrpc {

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item("Hello " + request.getName() + "!")
                .map(msg -> HelloReply.newBuilder().setMessage(msg).build());
    }

    // Use in IDE: Starts the app for development. Not used in production.
    public static void main(String... args) { io.quarkus.runtime.Quarkus.run(args); }

}
