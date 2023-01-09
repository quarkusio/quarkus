package io.quarkus.grpc.example.interceptors;

import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyCopycatGrpc;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.examples.interceptors.HelloExceptionHandlerProvider;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

class HelloWorldServiceTestBase {

    private Channel channel;
    private Vertx _vertx;

    protected Vertx vertx() {
        return null;
    }

    protected void close(Vertx vertx) {
    }

    @BeforeEach
    public void init() {
        _vertx = vertx();
        channel = GRPCTestUtils.channel(_vertx);
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
        close(_vertx);
    }

    private static void assertException(Throwable t) {
        // TODO
        System.out.println("Exception > " + t);
        Assertions.assertTrue(HelloExceptionHandlerProvider.invoked);
    }

    @Test
    public void testExceptionHandlerObserver() throws Exception {
        HelloExceptionHandlerProvider.invoked = false;
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        GreeterGrpc.GreeterStub client = GreeterGrpc.newStub(channel);
        StreamObserver<HelloRequest> requests = client.multiHello(new StreamObserver<>() {
            @Override
            public void onNext(HelloReply helloReply) {
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    assertException(throwable);
                    failed.complete(true);
                } catch (Throwable t) {
                    failed.completeExceptionally(t);
                }
            }

            @Override
            public void onCompleted() {
            }
        });
        requests.onNext(HelloRequest.newBuilder().setName("Test").build());
        requests.onNext(HelloRequest.newBuilder().setName("Fail").build());
        //requests.onCompleted(); // onError should complete it
        if (!failed.get(10, TimeUnit.SECONDS)) {
            fail("Should fail!");
        }
    }

    @Test
    public void testExceptionHandlerOne() {
        HelloExceptionHandlerProvider.invoked = false;
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        try {
            //noinspection ResultOfMethodCallIgnored
            client.sayHello(HelloRequest.newBuilder().setName("Fail").build());
            fail("Should fail!");
        } catch (Exception e) {
            assertException(e);
        }
    }

    @Test
    public void testExceptionHandlerMultiMutiny() throws Exception {
        HelloExceptionHandlerProvider.invoked = false;
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        MutinyCopycatGrpc.MutinyCopycatStub client = MutinyCopycatGrpc.newMutinyStub(channel);
        List<HelloRequest> requests = List.of(
                HelloRequest.newBuilder().setName("Test").build(),
                HelloRequest.newBuilder().setName("Fail").build());
        Multi<HelloReply> response = client.multiCat(Multi.createFrom().items(requests.stream()));
        response.subscribe().with(
                r -> System.out.println(r.toString()),
                t -> {
                    assertException(t);
                    failed.complete(true);
                },
                () -> failed.complete(false));
        if (!failed.get(10, TimeUnit.SECONDS)) {
            fail("Should fail!");
        }
    }

    @Test
    public void testExceptionHandlerUniMutiny() throws Exception {
        HelloExceptionHandlerProvider.invoked = false;
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        MutinyCopycatGrpc.MutinyCopycatStub client = MutinyCopycatGrpc.newMutinyStub(channel);
        Uni<HelloReply> response = client.sayCat(HelloRequest.newBuilder().setName("Fail").build());
        response.subscribe().with(
                r -> {
                },
                t -> {
                    assertException(t);
                    failed.complete(true);
                });
        if (!failed.get(10, TimeUnit.SECONDS)) {
            fail("Should fail!");
        }
    }
}
