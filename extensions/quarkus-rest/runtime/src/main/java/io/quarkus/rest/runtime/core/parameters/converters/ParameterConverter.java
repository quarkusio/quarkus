package io.quarkus.rest.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.quarkus.rest.runtime.core.ParamConverterProviders;

public interface ParameterConverter {

    Object convert(Object parameter);

    void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations);
}
