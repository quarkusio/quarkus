package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Context;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.stork.api.ServiceInstance;

class GrpcStorkContextDetachTestBase {

    private static final Context.Key<Boolean> STORK_MEASURE_TIME = contextKey("STORK_MEASURE_TIME");
    private static final Context.Key<AtomicReference<ServiceInstance>> STORK_SERVICE_INSTANCE = contextKey(
            "STORK_SERVICE_INSTANCE");

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

    @SuppressWarnings("unchecked")
    private static <T> Context.Key<T> contextKey(String fieldName) {
        try {
            Class<?> collectorClass = Class.forName(
                    "io.quarkus.grpc.runtime.stork.StorkMeasuringCollector",
                    true,
                    Thread.currentThread().getContextClassLoader());
            Field field = collectorClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Context.Key<T>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access StorkMeasuringCollector." + fieldName, e);
        }
    }
}
