package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.Period;

public class PeriodParamConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        return Period.parse(value.toString());
    }

    public static final class Supplier implements ParameterConverterSupplier {

        @Override
        public ParameterConverter get() {
            return new PeriodParamConverter();
        }

        @Override
        public String getClassName() {
            return PeriodParamConverter.class.getName();
        }
    }
}
