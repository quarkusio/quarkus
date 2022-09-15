package org.jboss.resteasy.reactive.server.core.parameters.converters;

import jakarta.ws.rs.ext.ParamConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class RuntimeParameterConverter implements ParameterConverter {

    private final ParamConverter<?> converter;

    public RuntimeParameterConverter(ParamConverter<?> converter) {
        this.converter = converter;
    }

    @Override
    public Object convert(Object value) {
        return converter.fromString((String) value);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        // we're initialised in our constructor
    }

}
