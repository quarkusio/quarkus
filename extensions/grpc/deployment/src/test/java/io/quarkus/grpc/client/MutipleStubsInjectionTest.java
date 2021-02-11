package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Channel;
import io.grpc.examples.goodbyeworld.*;
import io.grpc.examples.helloworld.*;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.server.services.GoodbyeService;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class MutipleStubsInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyConsumer.class,
                            MutinyGreeterGrpc.class, GreeterGrpc.class,
                            MutinyGreeterGrpc.MutinyGreeterStub.class,
                            HelloService.class, HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class,
                            MutinyFarewellGrpc.class, FarewellGrpc.class,
                            MutinyFarewellGrpc.MutinyFarewellStub.class,
                            GoodbyeService.class, GoodbyeRequest.class, GoodbyeReply.class,
                            GoodbyeReplyOrBuilder.class, GoodbyeRequestOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invokeMutinyGreeter("neo-mutiny");
        assertThat(neo).isEqualTo("Hello neo-mutiny");

        neo = service.invokeBlockingGreeter("neo-blocking");
        assertThat(neo).isEqualTo("Hello neo-blocking");

        neo = service.invokeMutinyFarewell("neo-mutiny");
        assertThat(neo).isEqualTo("Goodbye neo-mutiny");

        neo = service.invokeBlockingFarewell("neo-blocking");
        assertThat(neo).isEqualTo("Goodbye neo-blocking");

        service.validateChannel();
    }

    @ApplicationScoped
    static class MyConsumer {

        @Inject
        @GrpcService("hello-service")
        MutinyGreeterGrpc.MutinyGreeterStub mutinyGreeter;

        @Inject
        @GrpcService("hello-service")
        GreeterGrpc.GreeterBlockingStub blockingGreeter;

        @Inject
        @GrpcService("hello-service")
        MutinyFarewellGrpc.MutinyFarewellStub mutinyFarewell;

        @Inject
        @GrpcService("hello-service")
        FarewellGrpc.FarewellBlockingStub blockingFarewell;

        @Inject
        @GrpcService("hello-service-2")
        Channel channel;

        public String invokeMutinyGreeter(String s) {
            return mutinyGreeter.sayHello(HelloRequest.newBuilder().setName(s).build())
                    .map(HelloReply::getMessage)
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String invokeBlockingGreeter(String s) {
            return blockingGreeter.sayHello(HelloRequest.newBuilder().setName(s).build()).getMessage();
        }

        public String invokeMutinyFarewell(String s) {
            return mutinyFarewell.sayGoodbye(GoodbyeRequest.newBuilder().setName(s).build())
                    .map(GoodbyeReply::getMessage)
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String invokeBlockingFarewell(String s) {
            return blockingFarewell.sayGoodbye(GoodbyeRequest.newBuilder().setName(s).build()).getMessage();
        }

        public void validateChannel() {
            assertThat(channel).isNotNull();
        }

    }
}
