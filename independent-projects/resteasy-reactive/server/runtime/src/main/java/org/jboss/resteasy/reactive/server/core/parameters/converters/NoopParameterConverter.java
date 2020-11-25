package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.server.core.ParamConverterProviders;

public class NoopParameterConverter implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        return parameter;
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {

    }

    public static class Supplier implements ParameterConverterSupplier {

        @Override
        public ParameterConverter get() {
            return new NoopParameterConverter();
        }

        @Override
        public String getClassName() {
            return NoopParameterConverter.class.getName();
        }

    }
}
