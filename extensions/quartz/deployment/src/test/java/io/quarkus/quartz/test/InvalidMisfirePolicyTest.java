package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidMisfirePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar.addClasses(MisfirePolicyTest.Jobs.class).addAsResource(new StringAsset(
                    "quarkus.quartz.misfire-policy.\"simple_invalid_misfire_policy\"=cron-trigger-do-nothing\n"
                            + "quarkus.quartz.misfire-policy.\"cron_invalid_misfire_policy\"=simple-trigger-reschedule-now-with-existing-repeat-count\n"),
                    "application.properties"));

    @Test
    public void shouldFailWhenInvalidMisfirePolicyConfiguration() {
        Assertions.fail();
    }

    static class Jobs {

        @Scheduled(identity = "simple_invalid_misfire_policy", every = "1s")
        void simpleInvalidMisfirePolicy() {
        }

        @Scheduled(identity = "cron_invalid_misfire_policy", cron = "0/1 * * * * ?")
        void cronInvalidMisfirePolicy() {
        }

    }
}
