package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidGlobalCronTriggerMisfirePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(
                    "quarkus.quartz.cron-trigger.misfire-policy=simple-trigger-reschedule-now-with-existing-repeat-count\n"),
                    "application.properties"));

    @Test
    public void shouldFailWhenInvalidMisfirePolicyConfiguration() {
        Assertions.fail();
    }
}
