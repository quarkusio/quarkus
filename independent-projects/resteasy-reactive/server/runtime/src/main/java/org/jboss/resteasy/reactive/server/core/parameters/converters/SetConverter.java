package org.jboss.resteasy.reactive.server.core.parameters.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.resteasy.reactive.server.core.ParamConverterProviders;

public class SetConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public SetConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        if (parameter instanceof List) {
            Set<Object> ret = new HashSet<>();
            List<String> values = (List<String>) parameter;
            for (String val : values) {
                if (delegate == null) {
                    ret.add(val);
                } else {
                    ret.add(delegate.convert(val));
                }
            }
            return ret;
        } else if (parameter == null) {
            return Collections.emptySet();
        } else if (delegate != null) {
            return Collections.singleton(delegate.convert(parameter));
        } else {
            return Collections.singleton(parameter);
        }
    }

    @Override
    public void init(ParamConverterProviders deployment, Class<?> rawType, Type genericType, Annotation[] annotations) {
        delegate.init(deployment, rawType, genericType, annotations);
    }

    public static class SetSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public SetSupplier() {
        }

        public SetSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getClassName() {
            return SetConverter.class.getName();
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new SetConverter(null) : new SetConverter(delegate.get());
        }

        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public SetSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
