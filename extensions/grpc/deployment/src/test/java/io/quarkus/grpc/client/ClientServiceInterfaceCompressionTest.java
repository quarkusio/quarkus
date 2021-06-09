package io.quarkus.grpc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterClient;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class ClientServiceInterfaceCompressionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(MyConsumer.class,
                            HelloService.class))
            .withConfigurationResource("hello-config-compression.properties");

    @Inject
    MyConsumer consumer;

    @Test
    public void testCallOptions() {
        GreeterClient client = (GreeterClient) consumer.service;
        assertEquals("gzip", client.getStub().getCallOptions().getCompressor());
    }

    @Singleton
    static class MyConsumer {

        @GrpcClient("hello-service")
        Greeter service;

    }
}
