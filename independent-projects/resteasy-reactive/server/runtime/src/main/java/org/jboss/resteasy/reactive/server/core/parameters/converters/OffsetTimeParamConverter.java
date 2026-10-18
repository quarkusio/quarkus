package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class OffsetTimeParamConverter extends TemporalParamConverter<OffsetTime> {

    // this can be called by generated code
    public OffsetTimeParamConverter() {
        super(DateTimeFormatter.ISO_OFFSET_TIME);
    }

    public OffsetTimeParamConverter(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected OffsetTime convert(String value) {
        return OffsetTime.parse(value);
    }

    @Override
    protected OffsetTime convert(String value, DateTimeFormatter formatter) {
        return OffsetTime.parse(value, formatter);
    }

    public static class Supplier extends TemporalSupplier<OffsetTimeParamConverter> {

        public Supplier() {
        }

        public Supplier(String pattern, String dateTimeFormatterProviderClassName) {
            super(pattern, dateTimeFormatterProviderClassName);
        }

        @Override
        protected OffsetTimeParamConverter createConverter(DateTimeFormatter dateTimeFormatter) {
            return new OffsetTimeParamConverter(dateTimeFormatter);
        }

        @Override
        public String getClassName() {
            return OffsetTimeParamConverter.class.getName();
        }
    }
}
