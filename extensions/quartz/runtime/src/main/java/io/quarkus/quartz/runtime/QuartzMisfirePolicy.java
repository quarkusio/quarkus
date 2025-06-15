package io.quarkus.quartz.runtime;

import java.util.EnumSet;
import java.util.Locale;

public enum QuartzMisfirePolicy {
    SMART_POLICY,
    IGNORE_MISFIRE_POLICY,
    FIRE_NOW,
    SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT,
    SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT,
    SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
    SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
    CRON_TRIGGER_DO_NOTHING;

    String dashedName() {
        return this.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    static EnumSet<QuartzMisfirePolicy> validCronValues() {
        return EnumSet.of(SMART_POLICY, IGNORE_MISFIRE_POLICY, FIRE_NOW, CRON_TRIGGER_DO_NOTHING);
    }

    static EnumSet<QuartzMisfirePolicy> validSimpleValues() {
        return EnumSet.of(SMART_POLICY, IGNORE_MISFIRE_POLICY, FIRE_NOW,
                SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT,
                SIMPLE_TRIGGER_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT,
                SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
                SIMPLE_TRIGGER_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
    }
}
