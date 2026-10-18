package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeParamConverter extends TemporalParamConverter<OffsetDateTime> {

    // this can be called by generated code
    public OffsetDateTimeParamConverter() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public OffsetDateTimeParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected OffsetDateTime convert(String value) {
        return OffsetDateTime.parse(value);
    }

    @Override
    protected OffsetDateTime convert(String value, DateTimeFormatter formatter) {
        return OffsetDateTime.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<OffsetDateTimeParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected OffsetDateTimeParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new OffsetDateTimeParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return OffsetDateTimeParamConverter.class.getName();
        }
    }
}
