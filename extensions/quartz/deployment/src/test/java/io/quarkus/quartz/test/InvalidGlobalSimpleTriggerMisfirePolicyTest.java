package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidGlobalSimpleTriggerMisfirePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.quartz.simple-trigger.misfire-policy=cron-trigger-do-nothing\n"),
                    "application.properties"));

    @Test
    public void shouldFailWhenInvalidMisfirePolicyConfiguration() {
        Assertions.fail();
    }
}
