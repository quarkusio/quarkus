package io.quarkus.quartz.test.metrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MpTimedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.metrics.enabled=true"),
                            "application.properties"));

    @Inject
    MetricRegistry metricRegistry;

    @Test
    void testTimedMethod() throws InterruptedException {
        assertTrue(Jobs.latch01.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.latch02.await(5, TimeUnit.SECONDS));
        Timer timer1 = metricRegistry
                .getTimer(new MetricID(Jobs.class.getName() + ".everySecond", new Tag("scheduled", "true")));
        assertNotNull(timer1);
        assertTrue(timer1.getCount() > 0);
        Timer timer2 = metricRegistry.getTimer(new MetricID(Jobs.class.getName() + ".foo"));
        assertNotNull(timer2);
        assertTrue(timer2.getCount() > 0);
    }

}
