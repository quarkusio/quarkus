package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class TimeTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{now.format('d uuuu')}:{nowLocalDate.format('d MMM uuuu',myLocale)}:{time:format(nowDate,'d MMM uuuu',myLocale)}:{time:format(nowCalendar,'d uuuu')}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testFormat() {
        Calendar nowCal = Calendar.getInstance();
        nowCal.set(Calendar.YEAR, 2020);
        nowCal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        nowCal.set(Calendar.DAY_OF_MONTH, 10);
        Date nowDate = nowCal.getTime();
        assertEquals("10 2020:10 Sep 2020:10 Sep 2020:10 2020", foo
                .data("now", LocalDateTime.of(2020, 9, 10, 11, 12))
                .data("nowLocalDate", LocalDate.of(2020, 9, 10))
                .data("nowDate", nowDate)
                .data("nowCalendar", nowCal)
                .data("myLocale", Locale.ENGLISH).render());
    }

}
