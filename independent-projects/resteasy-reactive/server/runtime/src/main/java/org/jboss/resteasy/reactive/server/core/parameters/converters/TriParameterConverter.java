package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import org.jboss.resteasy.reactive.Parameter;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class TriParameterConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public TriParameterConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (parameter == null) {
            return Parameter.absent();
        } else if (parameter instanceof String
                && ((String) parameter).isEmpty()) {
            return Parameter.cleared();
        } else if (parameter instanceof Collection list
                && list.isEmpty()) {
            return Parameter.absent();
        } else if (parameter instanceof Collection list
                && list.size() == 1
                && list.contains("")) {
            return Parameter.cleared();
        } else if (delegate != null) {
            Object converted = delegate.convert(parameter);
            if (converted != null
                    && converted instanceof Collection
                    // FIXME: this special case is fishy
                    && ((Collection) converted).isEmpty()) {
                return Parameter.absent();
            } else {
                return Parameter.ofNullable(converted);
            }
        } else {
            return Parameter.set(parameter);
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        if (delegate != null) {
            delegate.init(deployment, rawType, genericType, annotations);
        }
    }

    public static class TriParameterSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public TriParameterSupplier() {
        }

        // invoked by reflection for BeanParam in ClassInjectorTransformer
        public TriParameterSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new TriParameterConverter(null) : new TriParameterConverter(delegate.get());
        }

        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public TriParameterSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public String getClassName() {
            return TriParameterConverter.class.getName();
        }

    }

}
