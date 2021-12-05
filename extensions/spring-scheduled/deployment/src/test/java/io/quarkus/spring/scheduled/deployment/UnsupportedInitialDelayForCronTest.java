package io.quarkus.spring.scheduled.deployment;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedInitialDelayForCronTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnsupportedInitialDelayForCronTest.InvalidBean.class));

    @Test
    public void test() {
        // This method should not be invoked
        Assertions.fail();
    }

    @ApplicationScoped
    static class InvalidBean {

        @Scheduled(cron = "0/1 * * * * ?", initialDelay = 60000)
        void checkEverySecondCronStartingInOneMinute() {
        }
    }

}
