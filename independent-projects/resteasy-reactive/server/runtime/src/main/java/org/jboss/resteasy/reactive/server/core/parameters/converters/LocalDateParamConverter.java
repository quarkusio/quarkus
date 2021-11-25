package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateParamConverter extends TemporalParamConverter<LocalDate> {

    public LocalDateParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected LocalDate convert(String value) {
        return LocalDate.parse(value);
    }

    @Override
    protected LocalDate convert(String value, DateTimeFormatter formatter) {
        return LocalDate.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<LocalDateParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected LocalDateParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new LocalDateParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return LocalDateParamConverter.class.getName();
        }
    }
}
