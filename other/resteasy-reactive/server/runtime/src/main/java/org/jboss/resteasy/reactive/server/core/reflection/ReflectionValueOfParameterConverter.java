package org.jboss.resteasy.reactive.server.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class ReflectionValueOfParameterConverter implements ParameterConverter {

    public ReflectionValueOfParameterConverter(Method valueOf) {
        this.valueOf = valueOf;
    }

    final Method valueOf;

    @Override
    public Object convert(Object parameter) {
        try {
            return valueOf.invoke(null, parameter);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {

    }
}
