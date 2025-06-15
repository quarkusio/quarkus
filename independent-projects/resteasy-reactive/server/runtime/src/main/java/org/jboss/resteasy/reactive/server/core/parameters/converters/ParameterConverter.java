package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public interface ParameterConverter {

    Object convert(Object parameter);

    default void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType,
            Annotation[] annotations) {

    }

    // TODO: this API method may be too limiting
    default boolean isForSingleObjectContainer() {
        return false;
    }
}
