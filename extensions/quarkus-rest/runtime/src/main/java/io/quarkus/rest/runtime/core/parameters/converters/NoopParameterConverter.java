package io.quarkus.rest.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.quarkus.rest.runtime.core.ParamConverterProviders;

public class NoopParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        return parameter;
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {

    }
}
