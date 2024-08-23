package io.quarkus.grpc.examples.hello;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

@Singleton
public class GrpcServices {

    @Inject
    Vertx vertx;

    static volatile long delayMs;

    List<VertxServer> servers = new CopyOnWriteArrayList<>();

    void shutDown(@Observes ShutdownEvent ev) {
        for (VertxServer server : servers) {
            server.shutdown();
        }
    }

    void setUp(@Observes StartupEvent ev) throws ExecutionException, InterruptedException {
        startServer(() -> 300L, "moderately-slow", 9013);
        startServer(() -> delayMs, "configurable", 9012);
    }

    private void startServer(Supplier<Long> delay, String name, int port) throws InterruptedException, ExecutionException {
        GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
            @Override
            public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
                long d = delay.get();
                if (d == 0) {
                    responseObserver.onNext(HelloReply.newBuilder().setMessage(name).build());
                    responseObserver.onCompleted();
                } else {
                    vertx.setTimer(delay.get(), l -> {
                        responseObserver.onNext(HelloReply.newBuilder().setMessage(name).build());
                        responseObserver.onCompleted();
                    });
                }
            }

            @Override
            public StreamObserver<HelloRequest> streamHello(StreamObserver<HelloReply> responseObserver) {
                return new StreamObserver<>() {
                    @Override
                    public void onNext(HelloRequest value) {
                        responseObserver.onNext(HelloReply.newBuilder().setMessage(name + "-stream").build());
                    }

                    @Override
                    public void onError(Throwable t) {
                        responseObserver.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                        responseObserver.onCompleted();
                    }
                };
            }
        };

        CompletableFuture<Void> result = new CompletableFuture<>();

        servers.add(VertxServerBuilder.forAddress(vertx, "localhost", port)
                .addService(service).build().start(r -> {
                    if (r.failed()) {
                        result.completeExceptionally(r.cause());
                    } else {
                        System.out.println("Started test gRPC server at port " + port);
                        result.complete(null);
                    }
                }));

        result.get();
    }
}
