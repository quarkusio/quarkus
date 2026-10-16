package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public record MultivaluedMapConverter(ParameterConverter delegate) implements ParameterConverter {

    @Override
    public Object convert(Object parameter) {
        if (parameter instanceof MultivaluedMap) {
            return parameter;
        }

        return new QuarkusMultivaluedHashMap<>();
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        if (delegate != null)
            delegate.init(deployment, rawType, genericType, annotations);
    }

    public static class MultivaluedMapSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public MultivaluedMapSupplier() {
        }

        public MultivaluedMapSupplier(ParameterConverterSupplier converter) {
            this.delegate = converter;
        }

        @Override
        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        @Override
        public String getClassName() {
            return MultivaluedMapConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new MultivaluedMapConverter(null) : new MultivaluedMapConverter(delegate.get());
        }

        public MultivaluedMapSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
