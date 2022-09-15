package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class MisfirePolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.misfire-policy.\"simple_ignore_misfire_policy\"=ignore-misfire-policy\n" +
                                    "quarkus.quartz.misfire-policy.\"cron_ignore_misfire_policy\"=ignore-misfire-policy\n" +
                                    "quarkus.quartz.misfire-policy.\"simple_reschedule_now_existing_policy\"=simple-trigger-reschedule-now-with-existing-repeat-count\n"
                                    +
                                    "quarkus.quartz.misfire-policy.\"simple_reschedule_now_remaining_policy\"=simple-trigger-reschedule-now-with-remaining-repeat-count\n"
                                    +
                                    "quarkus.quartz.misfire-policy.\"simple_reschedule_next_existing_policy\"=simple-trigger-reschedule-next-with-existing-count\n"
                                    +
                                    "quarkus.quartz.misfire-policy.\"simple_reschedule_next_remaining_policy\"=simple-trigger-reschedule-next-with-remaining-count\n"
                                    +
                                    "quarkus.quartz.misfire-policy.\"cron_do_nothing_policy\"=cron-trigger-do-nothing\n"

                    ), "application.properties"));

    @Inject
    org.quartz.Scheduler quartz;

    @Test
    public void testDefaultMisfirePolicy() throws SchedulerException {
        Trigger defaultMisfirePolicy = quartz
                .getTrigger(new TriggerKey("default_misfire_policy", Scheduler.class.getName()));
        assertNotNull(defaultMisfirePolicy);
        assertEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, defaultMisfirePolicy.getMisfireInstruction());

    }

    @Test
    public void testIgnoreMisfirePolicy() throws SchedulerException {
        Trigger simpleIgnoreMisfirePolicyTrigger = quartz
                .getTrigger(new TriggerKey("simple_ignore_misfire_policy", Scheduler.class.getName()));
        assertNotNull(simpleIgnoreMisfirePolicyTrigger);
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                simpleIgnoreMisfirePolicyTrigger.getMisfireInstruction());

        Trigger cronIgnoreMisfirePolicyTrigger = quartz
                .getTrigger(new TriggerKey("cron_ignore_misfire_policy", Scheduler.class.getName()));
        assertNotNull(cronIgnoreMisfirePolicyTrigger);
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, cronIgnoreMisfirePolicyTrigger.getMisfireInstruction());
    }

    @Test
    public void testSimpleTriggerMisfirePolicy() throws SchedulerException {
        Trigger simpleRescheduleNowExistingPolicy = quartz
                .getTrigger(new TriggerKey("simple_reschedule_now_existing_policy", Scheduler.class.getName()));
        assertNotNull(simpleRescheduleNowExistingPolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT,
                simpleRescheduleNowExistingPolicy.getMisfireInstruction());

        Trigger simpleRescheduleNowRemainingPolicy = quartz
                .getTrigger(new TriggerKey("simple_reschedule_now_remaining_policy", Scheduler.class.getName()));
        assertNotNull(simpleRescheduleNowRemainingPolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT,
                simpleRescheduleNowRemainingPolicy.getMisfireInstruction());

        Trigger simpleRescheduleNextExistingPolicy = quartz
                .getTrigger(new TriggerKey("simple_reschedule_next_existing_policy", Scheduler.class.getName()));
        assertNotNull(simpleRescheduleNextExistingPolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
                simpleRescheduleNextExistingPolicy.getMisfireInstruction());

        Trigger simpleRescheduleNextRemainingPolicy = quartz
                .getTrigger(new TriggerKey("simple_reschedule_next_remaining_policy", Scheduler.class.getName()));
        assertNotNull(simpleRescheduleNextRemainingPolicy);
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
                simpleRescheduleNextRemainingPolicy.getMisfireInstruction());
    }

    @Test
    public void testCronTriggerMisfirePolicy() throws SchedulerException {
        Trigger cronDoNothingPolicy = quartz.getTrigger(new TriggerKey("cron_do_nothing_policy", Scheduler.class.getName()));
        assertNotNull(cronDoNothingPolicy);
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cronDoNothingPolicy.getMisfireInstruction());
    }

    static class Jobs {

        @Scheduled(identity = "default_misfire_policy", every = "1s")
        void defaultMisfirePolicy() {
        }

        @Scheduled(identity = "simple_ignore_misfire_policy", every = "1s")
        void simpleIgnoreMisfirePolicy() {
        }

        @Scheduled(identity = "cron_ignore_misfire_policy", cron = "0/1 * * * * ?")
        void cronIgnoreMisfirePolicy() {
        }

        @Scheduled(identity = "simple_reschedule_now_existing_policy", every = "1s")
        void simpleRescheduleNowExistingPolicy() {
        }

        @Scheduled(identity = "simple_reschedule_now_remaining_policy", every = "1s")
        void simpleRescheduleNowRemainingPolicy() {
        }

        @Scheduled(identity = "simple_reschedule_next_existing_policy", every = "1s")
        void simpleRescheduleNextExistingPolicy() {
        }

        @Scheduled(identity = "simple_reschedule_next_remaining_policy", every = "1s")
        void simpleRescheduleNextRemainingPolicy() {
        }

        @Scheduled(identity = "cron_do_nothing_policy", cron = "0/1 * * * * ?")
        void cronDoNothingPolicy() {
        }
    }
}
