package io.quarkus.grpc.server.duplicates;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * A subclass of the Mutiny ImplBase and a subclass of the plain grpc-java ImplBase for the same service.
 */
public class MutinyImplBaseAndImplBaseDuplicateTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, MutinyImplBaseHelloService.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @GrpcService
    public static class MutinyImplBaseHelloService extends MutinyGreeterGrpc.GreeterImplBase {

        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hi " + request.getName()).build());
        }
    }
}
