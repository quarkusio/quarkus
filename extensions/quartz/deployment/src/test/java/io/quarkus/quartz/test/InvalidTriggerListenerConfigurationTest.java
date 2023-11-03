package io.quarkus.quartz.test;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidTriggerListenerConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalArgumentException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleJobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.trigger-listeners.jobHistory.class=org.quartz.plugins.history.LoggingJobHistoryPlugin\n"
                                    + "quarkus.quartz.trigger-listeners.jobHistory.properties.jobSuccessMessage=Job [{1}.{0}] execution complete and reports: {8}"),
                            "application.properties"));

    @Test
    public void shouldFailWhenInvalidTriggerListenerConfiguration() {
        Assertions.fail();
    }
}
