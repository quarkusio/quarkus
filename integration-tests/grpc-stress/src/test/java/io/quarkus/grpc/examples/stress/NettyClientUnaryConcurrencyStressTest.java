package io.quarkus.grpc.examples.stress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Stress test using the classic gRPC Java (Netty) client via {@link GRPCTestUtils}.
 * <p>
 * This complements {@link VertxClientUnaryConcurrencyStressTest} to compare transport
 * reliability under concurrent unary load (see <a href="https://github.com/quarkusio/quarkus/issues/53847">#53847</a>).
 */
@QuarkusTest
class NettyClientUnaryConcurrencyStressTest {

    private Channel channel;

    @BeforeEach
    void setUp() {
        channel = GRPCTestUtils.channel(null);
    }

    @AfterEach
    void tearDown() {
        GRPCTestUtils.close(channel);
    }

    @Test
    void concurrentBlockingUnaryCallsDoNotDropMessages() throws InterruptedException {
        GrpcStressSupport.runConcurrentUnaryEcho(StressGrpc.newBlockingStub(channel));
    }

    @Test
    void largePayloadUnaryCallsSucceed() {
        GrpcStressSupport.runLargePayloadEcho(StressGrpc.newBlockingStub(channel),
                GrpcStressSupport.oneMegabytePayload());
    }
}
