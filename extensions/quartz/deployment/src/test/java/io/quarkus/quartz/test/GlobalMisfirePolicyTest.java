package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalMisfirePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.cron-trigger.misfire-policy=fire-now\n" +
                                    "quarkus.quartz.simple-trigger.misfire-policy=smart-policy\n"),
                            "application.properties"));

    @Inject
    org.quartz.Scheduler quartz;

    @Test
    public void testGlobalCronMisfirePolicy() throws SchedulerException {
        Trigger defaultMisfirePolicy = quartz
                .getTrigger(new TriggerKey("cron_trigger_policy", Scheduler.class.getName()));
        assertNotNull(defaultMisfirePolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW, defaultMisfirePolicy.getMisfireInstruction());
    }

    @Test
    public void testGlobalSimpleMisfirePolicy() throws SchedulerException {
        Trigger smartPolicy = quartz
                .getTrigger(new TriggerKey("simple_trigger_policy", Scheduler.class.getName()));
        assertNotNull(smartPolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY, smartPolicy.getMisfireInstruction());
    }

    static class Jobs {

        @Scheduled(identity = "cron_trigger_policy", cron = "0/1 * * * * ?")
        void cronTrigger() {
        }

        @Scheduled(identity = "simple_trigger_policy", every = "1s")
        void simpleTrigger() {
        }
    }
}
