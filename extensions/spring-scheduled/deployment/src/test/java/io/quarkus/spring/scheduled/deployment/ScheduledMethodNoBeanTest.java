package io.quarkus.spring.scheduled.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class ScheduledMethodNoBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NoBean.class));

    @Test
    public void test() throws InterruptedException {
        // Only assert that the scheduled method is executed
        assertFalse(NoBean.LATCH.await(5, TimeUnit.SECONDS));

    }

    static class NoBean {

        static final CountDownLatch LATCH = new CountDownLatch(4);

        @Scheduled(cron = "0/1 * * * * ?")
        void checkEverySecondCronStartingInOneMinute() {
            LATCH.countDown();
        }
    }

}
