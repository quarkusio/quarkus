package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.time.Instant;

public class InstantParamConverter implements ParameterConverter {

    @Override
    public Object convert(Object value) {
        return Instant.parse(value.toString());
    }

    public static class Supplier implements ParameterConverterSupplier {

        public Supplier() {
        }

        @Override
        public ParameterConverter get() {
            return new InstantParamConverter();
        }

        @Override
        public String getClassName() {
            return InstantParamConverter.class.getName();
        }
    }
}
