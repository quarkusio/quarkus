package io.quarkus.scheduler.test;

import javax.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot((jar) -> jar
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
