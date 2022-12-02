package io.quarkus.spring.scheduled.deployment;

import static org.wildfly.common.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class RepeatedScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ScheduledBean.class));

    @Test
    public void test() throws InterruptedException {
        // Only assert that the scheduled method is executed
        assertTrue(ScheduledBean.LATCH.await(5, TimeUnit.SECONDS));

    }

    @ApplicationScoped
    static class ScheduledBean {

        static final CountDownLatch LATCH = new CountDownLatch(4);

        @Scheduled(cron = "0/1 * * * * ?")
        @Scheduled(cron = "0/3 * * * * ?")
        void countDown() {
            LATCH.countDown();
        }

    }
}
