package io.quarkus.scheduler.common.runtime.util;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.scheduler.Scheduled;

public class SchedulerUtilsTest {

    @Test
    void testIsConfigValueCurlyBracesSyntax() {
        Assertions.assertTrue(SchedulerUtils.isConfigValue("{my.property:0 15 10 * * ?} "));
    }

    @Test
    void testIsConfigValueJBossStyle() {
        Assertions.assertTrue(SchedulerUtils.isConfigValue(" ${myMethod.cron.expr}"));
    }

    @Test
    void testIsConfigValueJBossStyleNested() {
        Assertions.assertTrue(SchedulerUtils.isConfigValue("${myMethod.cron.expr:${myMethod.cron.default}}"));
    }

    @Test
    void testIsConfigValueLiteralExpression() {
        Assertions.assertFalse(SchedulerUtils.isConfigValue("0 15 10 * * ?"));
    }

    @Test
    void testIsOffCronExpr() {
        Assertions.assertFalse(SchedulerUtils.isOff("0 15 10 * * ?"));
    }

    @Test
    void testIsOffNullValue() {
        Assertions.assertFalse(SchedulerUtils.isOff(null));
    }

    @Test
    void testIsOffCaseInsensitive() {
        Assertions.assertTrue(SchedulerUtils.isOff("OfF"));
    }

    @Test
    void testIsOffDisabledCaseInsensitive() {
        Assertions.assertTrue(SchedulerUtils.isOff("dIsAbLeD"));
    }

    @Test
    void testDefaultValueCurlyBraces() {
        Assertions.assertEquals("MY_DEFAULT_VALUE",
                SchedulerUtils.lookUpPropertyValue("{non.existing.property:MY_DEFAULT_VALUE}"));
    }

    @Test
    void testDefaultValueJBossStyle() {
        Assertions.assertEquals("MY_DEFAULT_VALUE",
                SchedulerUtils.lookUpPropertyValue("${non.existing.property:MY_DEFAULT_VALUE}"));
    }

    @Test
    void testDefaultValueNestedJBossStyle() {
        Assertions.assertEquals("MY_DEFAULT_VALUE",
                SchedulerUtils.lookUpPropertyValue("${non.existing.property1:${non.existing.property2:MY_DEFAULT_VALUE}}"));
    }

    @Test
    void testParseDurationPeriodString() {
        Assertions.assertEquals(24 * 60 * 60 * 1000, SchedulerUtils.parseDelayedAsMillis(createScheduledDelayed("P1D")));
    }

    @Test
    void testParseDurationPeriod() {
        Assertions.assertEquals(24 * 60 * 60 * 1000, SchedulerUtils.parseDelayedAsMillis(createScheduledDelayed("1d")));
    }

    @Test
    void testParseDurationPeriodOfTimeString() {
        Assertions.assertEquals(2 * 60 * 60 * 1000, SchedulerUtils.parseDelayedAsMillis(createScheduledDelayed("PT2H")));
    }

    @Test
    void testParseDurationPeriodOfTime() {
        Assertions.assertEquals(2 * 60 * 60 * 1000, SchedulerUtils.parseDelayedAsMillis(createScheduledDelayed("2h")));
    }

    private Scheduled createScheduledDelayed(String delayed) {
        return new Scheduled() {

            public Class<? extends Annotation> annotationType() {
                return null;
            }

            public long delay() {
                return 0;
            }

            public TimeUnit delayUnit() {
                return TimeUnit.MINUTES;
            }

            public ConcurrentExecution concurrentExecution() {
                return ConcurrentExecution.PROCEED;
            }

            public String timeZone() {
                return Scheduled.DEFAULT_TIMEZONE;
            }

            public String identity() {
                return "";
            }

            public String every() {
                return "";
            }

            public String cron() {
                return "";
            }

            public String overdueGracePeriod() {
                return "";
            }

            public Class<? extends SkipPredicate> skipExecutionIf() {
                return Never.class;
            }

            public String delayed() {
                return delayed;
            }

            @Override
            public String executeWith() {
                return AUTO;
            }

            @Override
            public String executionMaxDelay() {
                return "";
            }

        };
    }
}
