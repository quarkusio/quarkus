package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZonedDateTimeParamConverter extends TemporalParamConverter<ZonedDateTime> {

    // this can be called by generated code
    public ZonedDateTimeParamConverter() {
        super(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    public ZonedDateTimeParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected ZonedDateTime convert(String value) {
        return ZonedDateTime.parse(value);
    }

    @Override
    protected ZonedDateTime convert(String value, DateTimeFormatter formatter) {
        return ZonedDateTime.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<ZonedDateTimeParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected ZonedDateTimeParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new ZonedDateTimeParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return ZonedDateTimeParamConverter.class.getName();
        }
    }
}
