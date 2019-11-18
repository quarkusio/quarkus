package io.quarkus.scheduler.test;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanWithInvalidScheduledMethod.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class BeanWithInvalidScheduledMethod {

        @Scheduled(cron = "0/1 * * * * ?")
        String wrongMethod() {
            return "";
        }

    }

}
