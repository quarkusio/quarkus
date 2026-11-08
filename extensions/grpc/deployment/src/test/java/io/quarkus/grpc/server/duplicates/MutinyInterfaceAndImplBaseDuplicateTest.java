package io.quarkus.grpc.server.duplicates;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.GreeterGrpc;
import io.quarkus.grpc.server.services.HelloService;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The case from the issue: one implementation of the Mutiny service interface and one subclass of the plain
 * grpc-java ImplBase for the same service.
 */
public class MutinyInterfaceAndImplBaseDuplicateTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(GreeterGrpc.class.getPackage())
                    .addClasses(HelloService.class, MutinyHelloService.class))
            .assertException(t -> {
                assertInstanceOf(DeploymentException.class, t);
                assertTrue(t.getMessage().contains(HelloService.class.getName()), t.getMessage());
                assertTrue(t.getMessage().contains(MutinyHelloService.class.getName()), t.getMessage());
            });

    @Test
    public void test() {
        fail("Should never have been called");
    }
}
