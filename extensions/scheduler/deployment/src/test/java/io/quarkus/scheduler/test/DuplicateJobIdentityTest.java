package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class DuplicateJobIdentityTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class));

    @Test
    public void test() {
        fail("Should not reach here since job identity must be unique and a deployment exception should have been thrown");
    }

    static class Jobs {
        @Scheduled(cron = "0/1 * * * * ?", identity = "identity")
        void firstMethod() {
        }

        @Scheduled(cron = "0/1 * * * * ?", identity = "identity")
        void secondMethod() {
        }
    }

}
