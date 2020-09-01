package io.quarkus.rest.runtime.core.parameters.converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ListConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public ListConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
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

    public static class ListSupplier implements Supplier<ParameterConverter> {
        private Supplier<ParameterConverter> delegate;

        public ListSupplier() {
        }

        public ListSupplier(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new ListConverter(null) : new ListConverter(delegate.get());
        }

        public Supplier<ParameterConverter> getDelegate() {
            return delegate;
        }

        public ListSupplier setDelegate(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
