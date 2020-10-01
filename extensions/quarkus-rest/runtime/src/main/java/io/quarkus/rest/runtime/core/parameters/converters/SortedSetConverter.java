package io.quarkus.rest.runtime.core.parameters.converters;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortedSetConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public SortedSetConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
        return convert(parameter, delegate);
    }

    public static Object convert(Object parameter, ParameterConverter delegate) {
        if (parameter instanceof List) {
            SortedSet<Object> ret = new TreeSet<>();
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
            return Collections.emptySortedSet();
        } else if (delegate != null) {
            SortedSet<Object> ret = new TreeSet<>();
            ret.add(delegate.convert(parameter));
            return ret;
        } else {
            SortedSet<Object> ret = new TreeSet<>();
            ret.add(parameter);
            return ret;
        }
    }

    public static class SortedSetSupplier implements DelegatingParameterConverterSupplier {
        private ParameterConverterSupplier delegate;

        public SortedSetSupplier() {
        }

        public SortedSetSupplier(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new SortedSetConverter(null) : new SortedSetConverter(delegate.get());
        }

        public ParameterConverterSupplier getDelegate() {
            return delegate;
        }

        public SortedSetSupplier setDelegate(ParameterConverterSupplier delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public String getClassName() {
            return SortedSetConverter.class.getName();
        }

    }

}
