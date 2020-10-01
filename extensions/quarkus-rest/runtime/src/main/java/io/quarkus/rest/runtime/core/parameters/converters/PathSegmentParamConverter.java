package io.quarkus.rest.runtime.core.parameters.converters;

import io.quarkus.rest.runtime.util.PathSegmentImpl;

public class PathSegmentParamConverter implements ParameterConverter {
    @Override
    public Object convert(Object parameter) {
        return new PathSegmentImpl(parameter.toString(), true);
    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public String getClassName() {
            return PathSegmentParamConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return new PathSegmentParamConverter();
        }
    }
}
