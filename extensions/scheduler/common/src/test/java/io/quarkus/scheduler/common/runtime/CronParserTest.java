package io.quarkus.scheduler.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;

public class CronParserTest {

    @Test
    public void testMapUnixToQuartz() {
        CronParser parser = new CronParser(CronType.UNIX);
        Cron cron = parser.parse("10 14 * * 1");
        Cron quartz = parser.mapToQuartz(cron);
        assertEquals("0 10 14 ? * 2 *", quartz.asString());
    }

    @Test
    public void testMapQuartzToQuartz() {
        CronParser parser = new CronParser(CronType.QUARTZ);
        Cron cron = parser.parse("0 10 14 ? * 2 *");
        assertSame(cron, parser.mapToQuartz(cron));
    }

    @Test
    public void testMapCron4jToQuartz() {
        CronParser parser = new CronParser(CronType.CRON4J);
        Cron cron = parser.parse("10 14 L * *");
        Cron quartz = parser.mapToQuartz(cron);
        assertEquals("0 10 14 L * ? *", quartz.asString());
    }

    @Test
    public void testMapSpringToQuartz() {
        CronParser parser = new CronParser(CronType.SPRING);
        Cron cron = parser.parse("1 10 14 * * 0");
        Cron quartz = parser.mapToQuartz(cron);
        assertEquals("1 10 14 ? * 1 *", quartz.asString());
    }

    @Test
    public void testMapSpring53ToQuartz() {
        CronParser parser = new CronParser(CronType.SPRING53);
        Cron cron = parser.parse("1 10 14 L * *");
        Cron quartz = parser.mapToQuartz(cron);
        assertEquals("1 10 14 L * ? *", quartz.asString());
    }

}
