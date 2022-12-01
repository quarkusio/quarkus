package io.quarkus.scheduler.test;

import javax.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidEveryExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidEveryExpressionTest.InvalidBean.class));

    @Test
    public void test() throws InterruptedException {
    }

    static class InvalidBean {

        @Scheduled(every = "call me every other day")
        void wrong() {
        }

    }

}
