package io.quarkus.scheduler.test;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleScheduledMethodsWithTheSameNameTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setExpectedException(DeploymentException.class)
            .withApplicationRoot((jar) -> jar.addClasses(BeanWithInvalidScheduledMethods.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class BeanWithInvalidScheduledMethods {

        @Scheduled(cron = "0/1 * * * * ?")
        void foo() {
        }

        @Scheduled(every = "10s")
        void foo(ScheduledExecution execution) {
        }

    }

}
