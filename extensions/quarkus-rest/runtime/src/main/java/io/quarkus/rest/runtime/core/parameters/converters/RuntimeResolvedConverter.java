package io.quarkus.rest.runtime.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.runtime.core.ParamConverterProviders;
import io.quarkus.rest.runtime.model.ResourceParamConverterProvider;

public class RuntimeResolvedConverter implements ParameterConverter {

    private volatile ParamConverter<?> converter;
    private ParameterConverter delegate;

    public RuntimeResolvedConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (converter != null)
            return converter.fromString(parameter.toString());
        return delegate.convert(parameter);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        for (ResourceParamConverterProvider i : deployment.getParamConverterProviders()) {
            ParamConverterProvider instance = i.getFactory().createInstance().getInstance();
            ParamConverter<?> converter = instance.getConverter(rawType, genericType, annotations);
            if (converter != null) {
                this.converter = converter;
                break;
            }
        }
        if (converter == null && delegate == null) {
            throw new RuntimeException("Unable to create param converter for parameter " + genericType);
        }
    }

    public static class Supplier implements DelegatingParameterConverterSupplier {

        private ParameterConverterSupplier delegate;

        @Override
        public String getClassName() {
            return RuntimeResolvedConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return new RuntimeResolvedConverter(delegate == null ? null : delegate.get());
        }

        public ParameterConverterSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }
    }
}
