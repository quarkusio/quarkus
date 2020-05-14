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
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloReplyOrBuilder;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloRequestOrBuilder;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.runtime.annotations.GrpcService;
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
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invokeMutiny("neo-mutiny");
        assertThat(neo).isEqualTo("Hello neo-mutiny");

        neo = service.invokeBlocking("neo-blocking");
        assertThat(neo).isEqualTo("Hello neo-blocking");

        service.validateChannel();
    }

    @ApplicationScoped
    static class MyConsumer {

        @Inject
        @GrpcService("hello-service")
        MutinyGreeterGrpc.MutinyGreeterStub mutiny;

        @Inject
        @GrpcService("hello-service")
        GreeterGrpc.GreeterBlockingStub blocking;

        @Inject
        @GrpcService("hello-service-2")
        Channel channel;

        public String invokeMutiny(String s) {
            return mutiny.sayHello(HelloRequest.newBuilder().setName(s).build())
                    .map(HelloReply::getMessage)
                    .await().atMost(Duration.ofSeconds(5));
        }

        public String invokeBlocking(String s) {
            return blocking.sayHello(HelloRequest.newBuilder().setName(s).build()).getMessage();
        }

        public void validateChannel() {
            assertThat(channel).isNotNull();
        }

    }
}
