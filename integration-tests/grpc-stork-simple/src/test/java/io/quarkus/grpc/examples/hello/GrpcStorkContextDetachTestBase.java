package io.quarkus.grpc.examples.hello;

import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_MEASURE_TIME;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_SERVICE_INSTANCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;

class GrpcStorkContextDetachTestBase {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingStub;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyStub;

    @Test
    void shouldDetachStorkContextAfterBlockingGrpcCall() {
        assertStorkContextDetached();
        HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("context-blocking").build());
        assertThat(reply.getMessage()).isEqualTo("Hello context-blocking");
        assertStorkContextDetached();
    }

    @Test
    void shouldDetachStorkContextAfterMutinyGrpcCall() {
        assertStorkContextDetached();
        HelloReply reply = mutinyStub
                .sayHello(HelloRequest.newBuilder().setName("context-mutiny").build())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(reply.getMessage()).isEqualTo("Hello context-mutiny");
        assertStorkContextDetached();
    }

    @Test
    void shouldDetachStorkContextAfterMultipleGrpcCalls() {
        for (int i = 0; i < 5; i++) {
            assertStorkContextDetached();
            HelloReply reply = blockingStub.sayHello(HelloRequest.newBuilder().setName("context-" + i).build());
            assertThat(reply.getMessage()).isEqualTo("Hello context-" + i);
            assertStorkContextDetached();
        }
    }

    private static void assertStorkContextDetached() {
        assertThat(STORK_MEASURE_TIME.get()).isNull();
        assertThat(STORK_SERVICE_INSTANCE.get()).isNull();
    }
}
