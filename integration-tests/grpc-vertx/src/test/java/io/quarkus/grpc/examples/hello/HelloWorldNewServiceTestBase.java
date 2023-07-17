package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import examples.MutinyGreeterGrpc;
import io.grpc.Channel;
import io.quarkus.grpc.test.utils.GRPCTestUtils;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;

abstract class HelloWorldNewServiceTestBase {

    private Channel channel;
    private Vertx _vertx;

    protected Vertx vertx() {
        return null;
    }

    protected void close(Vertx vertx) {
    }

    protected abstract int port();

    protected void checkVerticles() {
    }

    protected void checkVerticles(Vertx vertx) {
        VertxInternal internal = (VertxInternal) vertx;
        Set<String> deploymentIDs = internal.deploymentIDs();
        // should be just one, but in the worst case skip the test if not
        Assumptions.assumeTrue(deploymentIDs.size() == 1);
        Deployment deployment = internal.getDeployment(deploymentIDs.iterator().next());
        Set<Verticle> verticles = deployment.getVerticles();
        Assumptions.assumeTrue(verticles.size() > 1);
    }

    protected boolean skipEventloopTest() {
        return false;
    }

    @BeforeEach
    public void init() {
        _vertx = vertx();
        channel = GRPCTestUtils.channel(_vertx, port());
    }

    @AfterEach
    public void cleanup() {
        GRPCTestUtils.close(channel);
        close(_vertx);
    }

    @Test
    public void testEventLoop() {
        // only check those where we know Vertx instance comes from Quarkus
        checkVerticles();

        Assumptions.assumeFalse(skipEventloopTest());

        Set<String> threadNames = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Channel newChannel = GRPCTestUtils.channel(_vertx, port());
            try {
                GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(newChannel);
                HelloReply reply = client.threadName(HelloRequest.newBuilder().build());
                threadNames.add(reply.getMessage());
            } finally {
                GRPCTestUtils.close(newChannel);
            }
        }
        assertThat(threadNames.size()).isGreaterThan(1);
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() {
        GreeterGrpc.GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        HelloReply reply = client
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build());
        assertThat(reply.getMessage()).isEqualTo("Hello neo-blocking");
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() {
        HelloReply reply = MutinyGreeterGrpc.newMutinyStub(channel)
                .sayHello(HelloRequest.newBuilder().setName("neo-blocking").build())
                .await().atMost(Duration.ofSeconds(5));
        assertThat(reply.getMessage()).isEqualTo("Hello neo-blocking");
    }

}
