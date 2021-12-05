package org.jboss.resteasy.reactive.server.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class ReflectionConstructorParameterConverter implements ParameterConverter {

    final Constructor constructor;

    public ReflectionConstructorParameterConverter(Constructor constructor) {
        this.constructor = constructor;
    }

    @Override
    public Object convert(Object parameter) {
        try {
            return constructor.newInstance(parameter);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {

    }
}
