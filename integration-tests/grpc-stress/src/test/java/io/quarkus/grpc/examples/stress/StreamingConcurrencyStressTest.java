package io.quarkus.grpc.examples.stress;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;

/**
 * Concurrent client-streaming stress using both Vert.x and Netty gRPC clients.
 */
@QuarkusTest
class StreamingConcurrencyStressTest {

    @Inject
    Vertx vertx;

    @Test
    void concurrentClientStreamingWithVertxClient() throws InterruptedException {
        Channel channel = GRPCTestUtils.channel(vertx);
        try {
            GrpcStressSupport.runConcurrentClientStreaming(channel);
        } finally {
            GRPCTestUtils.close(channel);
        }
    }

    @Test
    void concurrentClientStreamingWithNettyClient() throws InterruptedException {
        Channel channel = GRPCTestUtils.channel(null);
        try {
            GrpcStressSupport.runConcurrentClientStreaming(channel);
        } finally {
            GRPCTestUtils.close(channel);
        }
    }
}
