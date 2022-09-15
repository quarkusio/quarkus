package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class ChannelInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyConsumer.class, GreeterGrpc.class, GreeterGrpc.GreeterBlockingStub.class,
                            HelloService.class, HelloRequest.class, HelloReply.class,
                            HelloReplyOrBuilder.class, HelloRequestOrBuilder.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invoke("neo-channel");
        assertThat(neo).isEqualTo("Hello neo-channel");
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        Channel channel;

        public String invoke(String s) {
            return GreeterGrpc.newBlockingStub(channel)
                    .sayHello(HelloRequest.newBuilder().setName(s).build())
                    .getMessage();
        }

    }
}
