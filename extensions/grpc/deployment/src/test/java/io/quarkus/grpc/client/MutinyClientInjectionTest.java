package io.quarkus.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class MutinyClientInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(HelloService.class))
            .withConfigurationResource("hello-config.properties");

    @Inject
    MyConsumer service;

    @Test
    public void test() {
        String neo = service.invoke("neo-mutiny");
        assertThat(neo).matches("Hello neo-mutiny");
    }

    @ApplicationScoped
    static class MyConsumer {

        @GrpcClient("hello-service")
        Greeter service;

        public String invoke(String s) {
            return service.sayHello(HelloRequest.newBuilder().setName(s).build())
                    .map(HelloReply::getMessage)
                    .await().atMost(Duration.ofSeconds(5));
        }

    }
}
