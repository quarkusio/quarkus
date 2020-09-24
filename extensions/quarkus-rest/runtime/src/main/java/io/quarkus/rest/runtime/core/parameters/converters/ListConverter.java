package io.quarkus.rest.runtime.core.parameters.converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public ListConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        return convert(parameter, delegate);
    }

    public static Object convert(Object parameter, ParameterConverter delegate) {
        if (parameter instanceof List) {
            if (delegate == null) {
                return parameter;
            }
            List<Object> ret = new ArrayList<>();
            List<String> values = (List<String>) parameter;
            for (String val : values) {
                ret.add(delegate.convert(val));
            }
            return ret;
        } else if (parameter == null) {
            return Collections.emptyList();
        } else if (delegate != null) {
            return Collections.singletonList(delegate.convert(parameter));
        } else {
            return Collections.singletonList(parameter);
        }
    }

    public ParameterConverter getDelegate() {
        return delegate;
    }

    public static class ListSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public ListSupplier() {
        }

        public ListSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new ListConverter(null) : new ListConverter(delegate.get());
        }

        @Override
        public String getClassName() {
            return ListConverter.class.getName();
        }

        @Override
        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public ListSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
