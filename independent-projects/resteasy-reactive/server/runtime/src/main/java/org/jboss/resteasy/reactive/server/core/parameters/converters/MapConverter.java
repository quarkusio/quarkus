package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public record MapConverter(ParameterConverter delegate) implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        if (parameter instanceof Map) {
            return parameter;
        }

        return Map.of();
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        if (delegate != null)
            delegate.init(deployment, rawType, genericType, annotations);
    }

    public static class MapSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public MapSupplier() {
        }

        public MapSupplier(ParameterConverterSupplier converter) {
            this.delegate = converter;
        }

        @Override
        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        @Override
        public String getClassName() {
            return MapConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new MapConverter(null) : new MapConverter(delegate.get());
        }

        public MapConverter.MapSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
