package io.quarkus.scheduler.test;

import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset("simpleJobs.cron=0/1 * * * * ?\nsimpleJobs.every=1s"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        for (CountDownLatch latch : SimpleJobs.LATCHES.values()) {
            Assertions.assertTrue(latch.await(4, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testSchedulerTimer() throws InterruptedException {
        assertNotNull(scheduler);
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.startTimer(300, () -> latch.countDown());
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

}
