package io.quarkus.smallrye.reactivemessaging.wiring;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MissingIncomingConnectorDetectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(Foo.class))
            .overrideConfigKey("mp.messaging.incoming.a.connector", "missing")
            .setExpectedException(DeploymentException.class, true);

    @Test
    public void runTest() {
        fail("The expected DeploymentException was not thrown");
    }

    @ApplicationScoped
    public static class Foo {

        @Incoming("a")
        public void consume(Integer integer) {

        }

    }
}
