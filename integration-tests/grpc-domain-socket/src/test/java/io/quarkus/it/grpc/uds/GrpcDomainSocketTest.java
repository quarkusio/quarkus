package io.quarkus.it.grpc.uds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.timeouts.TestTimeouts;

@QuarkusTest
@WithTestResource(GrpcDomainSocketTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class GrpcDomainSocketTest {

    @GrpcClient("hello")
    Greeter greeter;

    @Test
    public void testGrpcOverDomainSocket() {
        HelloReply reply = greeter.sayHello(
                HelloRequest.newBuilder().setName("World").build())
                .await().atMost(TestTimeouts.get());
        assertThat(reply.getMessage()).isEqualTo("Hello World");
    }
}
