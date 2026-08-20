package io.quarkus.grpc.examples.stress;

import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Stress test using the Quarkus Vert.x-based gRPC client ({@code @GrpcClient}).
 */
@QuarkusTest
class VertxClientUnaryConcurrencyStressTest {

    @GrpcClient("stress")
    StressGrpc.StressBlockingStub blockingStub;

    @GrpcClient("stress")
    MutinyStressGrpc.MutinyStressStub mutinyStub;

    @Test
    void concurrentBlockingUnaryCallsDoNotDropMessages() throws InterruptedException {
        GrpcStressSupport.runConcurrentUnaryEcho(blockingStub);
    }

    @Test
    void concurrentMutinyUnaryCallsDoNotDropMessages() {
        GrpcStressSupport.runConcurrentUnaryEcho(mutinyStub);
    }

    @Test
    void largePayloadUnaryCallsSucceed() {
        GrpcStressSupport.runLargePayloadEcho(blockingStub, GrpcStressSupport.oneMegabytePayload());
    }
}
