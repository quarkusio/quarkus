package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeParamConverter extends TemporalParamConverter<LocalDateTime> {

    // this can be called by generated code
    public LocalDateTimeParamConverter() {
        super(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTimeParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected LocalDateTime convert(String value) {
        return LocalDateTime.parse(value);
    }

    @Override
    protected LocalDateTime convert(String value, DateTimeFormatter formatter) {
        return LocalDateTime.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<LocalDateTimeParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected LocalDateTimeParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new LocalDateTimeParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return LocalDateTimeParamConverter.class.getName();
        }
    }
}
