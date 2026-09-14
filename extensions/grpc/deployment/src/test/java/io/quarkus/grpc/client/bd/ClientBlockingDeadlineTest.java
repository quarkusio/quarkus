package io.quarkus.grpc.client.bd;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusExtensionTest;

public class ClientBlockingDeadlineTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(HelloService.class))
            .withConfigurationResource("hello-config-deadline.properties");

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub stub;

    @Test
    public void testCallOptions() {
        StatusRuntimeException sre = Assertions.assertThrows(StatusRuntimeException.class,
                () -> stub.sayHello(HelloRequest.newBuilder().setName("Scaladar").build()));
        Status status = sre.getStatus();
        Assertions.assertNotNull(status);
        Assertions.assertEquals(Status.DEADLINE_EXCEEDED.getCode(), status.getCode());
    }
}
