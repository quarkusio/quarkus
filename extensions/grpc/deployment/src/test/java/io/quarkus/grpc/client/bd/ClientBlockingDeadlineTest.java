package io.quarkus.grpc.client.bd;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.QuarkusUnitTest;

public class ClientBlockingDeadlineTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addClasses(HelloService.class))
            .withConfigurationResource("hello-config-deadline.properties");

    @GrpcClient("hello-service")
    GreeterGrpc.GreeterBlockingStub stub;

    @Test
    public void testCallOptions() {
        Deadline deadline = stub.getCallOptions().getDeadline();
        assertNotNull(deadline);
        try {
            //noinspection ResultOfMethodCallIgnored
            stub.sayHello(HelloRequest.newBuilder().setName("Scaladar").build());
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof StatusRuntimeException);
            StatusRuntimeException sre = (StatusRuntimeException) e;
            Status status = sre.getStatus();
            Assertions.assertNotNull(status);
            Assertions.assertEquals(Status.DEADLINE_EXCEEDED.getCode(), status.getCode());
        }
    }
}
