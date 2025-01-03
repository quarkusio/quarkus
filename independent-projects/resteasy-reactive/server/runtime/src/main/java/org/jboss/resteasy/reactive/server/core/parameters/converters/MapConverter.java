package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class MapConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public MapConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (parameter instanceof Map) {
            Map result = new HashMap<>();
            Map<String, String> input = (Map<String, String>) parameter;
            for (String key : input.keySet()) {
                result.put(key, input.get(key));
            }
            return result;
        }
        if (parameter == null) {
            return Collections.emptyMap();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        delegate.init(deployment, rawType, genericType, annotations);
    }

    @Override
    public boolean isForSingleObjectContainer() {
        return true;
    }

    public static class MapSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public MapSupplier() {
        }

        // invoked by reflection for BeanParam in ClassInjectorTransformer
        public MapSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getClassName() {
            return MapConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new MapConverter(null) : new MapConverter(delegate.get());
        }

        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public MapSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
