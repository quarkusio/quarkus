package io.quarkus.grpc.client.deadline.percall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The configured deadline applies to each call, so a stub stays usable after the deadline duration has elapsed
 * since it was created.
 */
public class ClientDeadlinePerCallTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(MyConsumer.class, SlowHelloService.class))
            .withConfigurationResource("hello-config-deadline.properties");

    @Inject
    MyConsumer consumer;

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub blockingStub;

    @Test
    public void blockingStubAppliesTheDeadlineToEachCall() throws InterruptedException {
        assertEquals("Hello fast", blockingStub.sayHello(request("fast")).getMessage());

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> blockingStub.sayHello(request("slow")));
        assertEquals(Status.DEADLINE_EXCEEDED.getCode(), exception.getStatus().getCode());

        Thread.sleep(500);

        assertEquals("Hello again", blockingStub.sayHello(request("again")).getMessage());
    }

    @Test
    public void mutinyClientAppliesTheDeadlineToEachCall() throws InterruptedException {
        assertEquals("Hello fast", await(consumer.service.sayHello(request("fast"))).getMessage());

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> await(consumer.service.sayHello(request("slow"))));
        assertEquals(Status.DEADLINE_EXCEEDED.getCode(), exception.getStatus().getCode());

        Thread.sleep(500);

        assertEquals("Hello again", await(consumer.service.sayHello(request("again"))).getMessage());
    }

    private static HelloRequest request(String name) {
        return HelloRequest.newBuilder().setName(name).build();
    }

    private static HelloReply await(io.smallrye.mutiny.Uni<HelloReply> uni) {
        return uni.await().atMost(Duration.ofSeconds(5));
    }

    @Singleton
    static class MyConsumer {

        @GrpcClient("hello-service")
        Greeter service;
    }
}
