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
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Two subclasses of the plain grpc-java ImplBase for the same service.
 */
public class TwoImplBaseDuplicateTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, AnotherHelloService.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @GrpcService
    public static class AnotherHelloService extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            responseObserver.onNext(HelloReply.newBuilder().setMessage("Hi " + request.getName()).build());
            responseObserver.onCompleted();
        }
    }
}
