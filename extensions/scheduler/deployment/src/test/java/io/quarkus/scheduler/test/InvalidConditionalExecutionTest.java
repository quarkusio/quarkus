package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidConditionalExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class, Some.class));

    @Test
    public void testExecution() {
        fail();
    }

    static class Jobs {

        // This is wrong - Some.class is not a bean
        @Scheduled(identity = "foo", every = "1s", skipExecutionIf = Some.class)
        void doSomething() throws InterruptedException {
        }

    }

    static class Some implements Scheduled.SkipPredicate {

        @Override
        public boolean test(ScheduledExecution execution) {
            return false;
        }

    }

}
