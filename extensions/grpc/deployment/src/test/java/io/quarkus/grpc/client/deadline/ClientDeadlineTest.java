package io.quarkus.grpc.client.deadline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Deadline;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterClient;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

public class ClientDeadlineTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(MyConsumer.class,
                            HelloService.class))
            .withConfigurationResource("hello-config-deadline.properties");

    @Inject
    MyConsumer consumer;

    @Test
    public void testCallOptions() {
        GreeterClient client = (GreeterClient) consumer.service;
        Deadline deadline = client.getStub().getCallOptions().getDeadline();
        assertNotNull(deadline);
        HelloReply reply = client.sayHello(HelloRequest.newBuilder().setName("Scaladar").build()).onFailure()
                .recoverWithItem(HelloReply.newBuilder().setMessage("ERROR!").build()).await().atMost(Duration.ofSeconds(5));
        assertEquals("ERROR!", reply.getMessage());
    }

    @Singleton
    static class MyConsumer {

        @GrpcClient("hello-service")
        Greeter service;

    }
}
