package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.server.core.ParamConverterProviders;

public interface ParameterConverter {

    Object convert(Object parameter);

    void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations);
}
