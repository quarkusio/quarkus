package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;

import org.junit.jupiter.api.Test;

public class LocaleConverterTest {

    @Test
    public void testStandardLocale() {
        LocaleConverter localeConverter = new LocaleConverter();
        Locale locale = localeConverter.convert("fr-FR");
        assertEquals("fr", locale.getLanguage());
        assertEquals("FR", locale.getCountry());
    }

    @Test
    public void testUnderscoreLocale() {
        LocaleConverter localeConverter = new LocaleConverter();
        Locale locale = localeConverter.convert("fr_FR");
        assertEquals("fr", locale.getLanguage());
        assertEquals("FR", locale.getCountry());
    }

    @Test
    public void testCLocale() {
        LocaleConverter localeConverter = new LocaleConverter();
        Locale locale = localeConverter.convert("c.u-US");
        assertSame(Locale.ROOT, locale);
    }

    @Test
    public void testEmptyLocale() {
        LocaleConverter localeConverter = new LocaleConverter();
        Locale locale = localeConverter.convert("");
        assertNull(locale);
    }

}
