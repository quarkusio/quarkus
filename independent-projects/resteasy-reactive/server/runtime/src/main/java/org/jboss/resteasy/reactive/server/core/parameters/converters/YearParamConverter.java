package org.jboss.resteasy.reactive.server.core.parameters.converters;

import static java.time.temporal.ChronoField.YEAR;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

public class YearParamConverter extends TemporalParamConverter<Year> {

    // lifted from the JDK as PARSER is private...
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD).toFormatter();

    // this can be called by generated code
    public YearParamConverter() {
        super(PARSER);
    }

    public YearParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected Year convert(String value) {
        return Year.parse(value);
    }

    @Override
    protected Year convert(String value, DateTimeFormatter formatter) {
        return Year.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<YearParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected YearParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new YearParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return YearParamConverter.class.getName();
        }
    }
}
