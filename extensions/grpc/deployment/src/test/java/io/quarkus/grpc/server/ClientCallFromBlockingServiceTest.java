package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld3.Greeter3Grpc;
import io.grpc.examples.helloworld3.HelloReply3;
import io.grpc.examples.helloworld3.HelloRequest3;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.grpc.server.services.GrpcCallWithinBlockingService;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusUnitTest;

public class ClientCallFromBlockingServiceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(Greeter3Grpc.class.getPackage())
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClass(HelloService.class)
                    .addClass(GrpcCallWithinBlockingService.class))
            .withConfigurationResource("call-from-blocking-service.properties");

    @Inject
    @GrpcService("service3")
    Greeter3Grpc.Greeter3BlockingStub greeter3Client;

    @Test
    @Timeout(5)
    void shouldWorkMultipleTimes() {
        for (int i = 0; i < 20; i++) {
            HelloReply3 reply = greeter3Client.sayHello(HelloRequest3.newBuilder().setName("Slim").build());
            assertThat(reply.getMessage()).isEqualTo("response:Hello Slim");
        }
    }
}
