package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeParamConverter extends TemporalParamConverter<LocalTime> {

    // this can be called by generated code
    public LocalTimeParamConverter() {
        super(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public LocalTimeParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected LocalTime convert(String value) {
        return LocalTime.parse(value);
    }

    @Override
    protected LocalTime convert(String value, DateTimeFormatter formatter) {
        return LocalTime.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<LocalTimeParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected LocalTimeParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new LocalTimeParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return LocalTimeParamConverter.class.getName();
        }
    }
}
