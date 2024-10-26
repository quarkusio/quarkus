package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;

import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    // can be tested with any Quartz property that we don't have a Quarkus equivalent for
                    // this test leverages org.quartz.context.key.SOME_KEY, which is a configuration equivalent of
                    // Scheduler.getContext().put("key", "value")
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.unsupported-properties.\"org.quartz.context.key.someKey\" = someValue\n" +
                                    "quarkus.scheduler.start-mode=forced"),
                            "application.properties"));

    @Inject
    org.quartz.Scheduler quartz;

    @Test
    public void testUnsupportedProperties() throws SchedulerException {
        SchedulerContext context = quartz.getContext();
        String key = "someKey";
        assertNotNull(context.get(key));
        assertEquals("someValue", context.get(key));
    }
}
