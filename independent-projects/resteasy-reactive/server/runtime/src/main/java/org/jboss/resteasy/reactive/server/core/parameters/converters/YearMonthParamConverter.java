package org.jboss.resteasy.reactive.server.core.parameters.converters;

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

public class YearMonthParamConverter extends TemporalParamConverter<YearMonth> {

    // lifted from the JDK as PARSER is private...
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .toFormatter();

    // this can be called by generated code
    public YearMonthParamConverter() {
        super(PARSER);
    }

    public YearMonthParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected YearMonth convert(String value) {
        return YearMonth.parse(value);
    }

    @Override
    protected YearMonth convert(String value, DateTimeFormatter formatter) {
        return YearMonth.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<YearMonthParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected YearMonthParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new YearMonthParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return YearMonthParamConverter.class.getName();
        }
    }
}
