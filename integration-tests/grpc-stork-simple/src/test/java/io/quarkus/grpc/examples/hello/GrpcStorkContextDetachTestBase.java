package io.quarkus.grpc.examples.hello;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Context;
import io.quarkus.grpc.GrpcClient;

class GrpcStorkContextDetachTestBase {

    private static final Context.Key<Boolean> STORK_MEASURE_TIME = Context.key("stork.measure-time");
    private static final Context.Key<Object> STORK_SERVICE_INSTANCE = Context.key("stork.service-instance");

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingStub;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyStub;

    @Test
    void shouldDetachStorkContextAfterBlockingGrpcCall() {
        assertStorkContextDetached();
        HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("context-blocking").build());
        assertEquals("Hello context-blocking", reply.getMessage());
        assertStorkContextDetached();
    }

    @Test
    void shouldDetachStorkContextAfterMutinyGrpcCall() {
        assertStorkContextDetached();
        HelloReply reply = mutinyStub
                .sayHello(HelloRequest.newBuilder().setName("context-mutiny").build())
                .await().atMost(Duration.ofSeconds(5));
        assertEquals("Hello context-mutiny", reply.getMessage());
        assertStorkContextDetached();
    }

    @Test
    void shouldDetachStorkContextAfterMultipleGrpcCalls() {
        for (int i = 0; i < 5; i++) {
            assertStorkContextDetached();
            HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("context-" + i).build());
            assertEquals("Hello context-" + i, reply.getMessage());
            assertStorkContextDetached();
        }
    }

    private static void assertStorkContextDetached() {
        assertNull(STORK_MEASURE_TIME.get());
        assertNull(STORK_SERVICE_INSTANCE.get());
    }
}
