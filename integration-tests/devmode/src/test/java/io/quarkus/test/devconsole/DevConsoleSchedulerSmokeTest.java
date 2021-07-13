package io.quarkus.test.devconsole;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleSchedulerSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(Jobs.class));

    @Test
    public void testScheduler() {
        RestAssured.get("q/dev")
                .then()
                .statusCode(200).body(Matchers.containsString("Schedules"));
        RestAssured.get("q/dev/io.quarkus.quarkus-scheduler/schedules")
                .then()
                .statusCode(200).body(Matchers.containsString("Scheduler is running"));
    }

    public static class Jobs {

        @Scheduled(every = "2h", delay = 2, delayUnit = TimeUnit.HOURS)
        public void run() {
        }

    }

}
