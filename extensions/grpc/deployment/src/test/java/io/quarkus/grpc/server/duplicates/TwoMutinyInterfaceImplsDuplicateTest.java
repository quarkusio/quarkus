package io.quarkus.grpc.server.duplicates;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.grpc.server.services.BlockingMutinyHelloService;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Two implementations of the Mutiny service interface for the same service.
 */
public class TwoMutinyInterfaceImplsDuplicateTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(MutinyHelloService.class, BlockingMutinyHelloService.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }
}
