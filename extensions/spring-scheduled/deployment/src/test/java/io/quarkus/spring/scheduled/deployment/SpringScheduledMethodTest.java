package io.quarkus.spring.scheduled.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SpringScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SpringScheduledMethodsBean.class)
                    .addAsResource(
                            new StringAsset(
                                    "springScheduledSimpleJobs.cron=0/1 * * * * ?\nspringScheduledSimpleJobs.fixedRate=1000"),
                            "application.properties"));

    @Test
    public void testSpringScheduledMethods() throws InterruptedException {
        for (CountDownLatch latch : SpringScheduledMethodsBean.LATCHES.values()) {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

}
