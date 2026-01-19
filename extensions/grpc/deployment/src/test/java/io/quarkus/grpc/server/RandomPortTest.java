package io.quarkus.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class RandomPortTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, HelloRequest.class, HelloReply.class))
            .overrideConfigKey("quarkus.grpc.server.test-port", "0");

    @Inject
    GrpcServer server;
    @Inject
    SmallRyeConfig config;
    @GrpcClient
    GreeterGrpc.GreeterBlockingStub client;

    @Test
    void ports() {
        assertTrue(server.getPort() > 0);
        assertNotEquals(9001, server.getPort());
        assertEquals(server.getPort(), config.getValue("quarkus.grpc.server.port", int.class));
        assertEquals(server.getPort(), config.getValue("quarkus.grpc.server.test-port", int.class));

        HelloReply reply = client.sayHello(HelloRequest.newBuilder().setName("Naruto").build());
        assertEquals("Hello Naruto", reply.getMessage());
    }
}
