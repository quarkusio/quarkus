package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class OptionalConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public OptionalConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (parameter == null) {
            return Optional.empty();
        } else if (delegate != null) {
            return Optional.ofNullable(delegate.convert(parameter));
        } else {
            return Optional.of(parameter);
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        if (delegate != null) {
            delegate.init(deployment, rawType, genericType, annotations);
        }
    }

    public static class OptionalSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public OptionalSupplier() {
        }

        public OptionalSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new OptionalConverter(null) : new OptionalConverter(delegate.get());
        }

        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public OptionalSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public String getClassName() {
            return OptionalConverter.class.getName();
        }

    }

}
