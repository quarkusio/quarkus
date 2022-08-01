package org.jboss.resteasy.reactive.server.core.parameters.converters;

public class CharacterParamConverter implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        String str = parameter.toString();
        if (str.length() != 1) {
            throw new IllegalArgumentException("invalid Character value: " + str);
        }
        return Character.valueOf(str.charAt(0));
    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public String getClassName() {
            return CharacterParamConverter.class.getName();
        }

        @Override
        public CharacterParamConverter get() {
            return new CharacterParamConverter();
        }
    }
}
