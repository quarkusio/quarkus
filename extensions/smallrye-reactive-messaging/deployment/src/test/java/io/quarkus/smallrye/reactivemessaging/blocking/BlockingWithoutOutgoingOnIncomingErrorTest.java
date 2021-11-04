package io.quarkus.smallrye.reactivemessaging.blocking;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.reactive.messaging.annotations.Blocking;

public class BlockingWithoutOutgoingOnIncomingErrorTest {

    @Inject
    BeanWithBlocking referenceToForceArcToUseTheBean;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithBlocking.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void runTest() {
        fail("The expected DeploymentException was not thrown");
    }

    @ApplicationScoped
    public static class BeanWithBlocking {
        @Blocking
        public String blockingMethod(String foo) {
            return foo + "1";
        }
    }
}
