package org.jboss.resteasy.reactive.server.core.parameters.converters;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.model.ResourceParamConverterProvider;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class RuntimeResolvedConverter implements ParameterConverter {

    private volatile ParamConverter<?> runtimeConverter;
    private final ParameterConverter quarkusConverter;

    public RuntimeResolvedConverter(ParameterConverter quarkusConverter) {
        this.quarkusConverter = quarkusConverter;
    }

    @Override
    public Object convert(Object parameter) {
        if (runtimeConverter != null)
            return runtimeConverter.fromString(parameter.toString());
        return quarkusConverter.convert(parameter);
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        for (ResourceParamConverterProvider i : deployment.getParamConverterProviders()) {
            ParamConverterProvider instance = i.getFactory().createInstance().getInstance();
            ParamConverter<?> converter = instance.getConverter(rawType, genericType, annotations);
            if (converter != null) {
                this.runtimeConverter = converter;
                break;
            }
        }
        if (runtimeConverter == null && quarkusConverter == null) {
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
