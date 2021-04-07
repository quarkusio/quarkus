package io.quarkus.scheduler.runtime.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
