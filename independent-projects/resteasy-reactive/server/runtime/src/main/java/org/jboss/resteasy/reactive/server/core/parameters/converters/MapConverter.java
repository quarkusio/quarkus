package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class MapConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public MapConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (parameter instanceof Map) {
            Map<Object, Object> ret = new HashMap<>();
            Map<?, ?> map = (Map<?, ?>) parameter;
            for (Map.Entry entry : map.entrySet()) {
                if (delegate == null) {
                    ret.put(entry.getKey(), entry.getValue());
                } else {
                    ret.put(entry.getKey(), delegate.convert(entry.getValue()));
                }
            }
            return ret;
        } else if (parameter instanceof MultivaluedMap<?, ?>) {
            MultivaluedMap<Object, Object> ret = new MultivaluedHashMap<>();
            MultivaluedMap<Object, Object> multivaluedMap = (MultivaluedMap<Object, Object>) parameter;
            for (Map.Entry<Object, List<Object>> entry : multivaluedMap.entrySet()) {
                List<Object> retValues = new ArrayList<>();
                for (Object value : entry.getValue()) {
                    if (delegate == null) {
                        retValues.add(value);
                    } else {
                        retValues.add(delegate.convert(value));
                    }
                }
                ret.put(entry.getKey(), retValues);
            }
            return ret;
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
