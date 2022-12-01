package io.quarkus.vertx.http.runtime.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DateTimeAttributeTest {

    @Test
    void shouldReadAttribute() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        DateTimeAttribute dta = new DateTimeAttribute("dd/MM/yyyy");
        assertThat(dta.readAttribute(null)).isEqualTo(sdf.format(new Date()));
    }

    @Test
    void testDefaultDateFormat() {
        final ExchangeAttribute attribute = new DateTimeAttribute.Builder().build(DateTimeAttribute.DATE_TIME_SHORT);
        final String value = attribute.readAttribute(null);
        Assertions.assertNotNull(value, DateTimeAttribute.DATE_TIME_SHORT + " attribute returned null");
        Assertions.assertFalse(value.trim().isEmpty(), DateTimeAttribute.DATE_TIME_SHORT + " attribute returned empty value");
    }

}
