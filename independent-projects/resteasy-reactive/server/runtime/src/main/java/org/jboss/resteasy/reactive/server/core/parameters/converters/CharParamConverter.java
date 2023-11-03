package org.jboss.resteasy.reactive.server.core.parameters.converters;

public class CharParamConverter implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        String str = parameter.toString();
        if (str.length() != 1) {
            throw new IllegalArgumentException("invalid char value: " + str);
        }
        return str.charAt(0);
    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public String getClassName() {
            return CharParamConverter.class.getName();
        }

        @Override
        public CharParamConverter get() {
            return new CharParamConverter();
        }
    }
}
