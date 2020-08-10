package io.quarkus.qrs.runtime.core.parameters.converters;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

public class SortedSetConverter implements ParameterConverter {

    private final ParameterConverter delegate;

    public SortedSetConverter(ParameterConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object convert(Object parameter) {
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
            return Collections.emptySet();
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

    public static class SortedSetSupplier implements Supplier<ParameterConverter> {
        private Supplier<ParameterConverter> delegate;

        public SortedSetSupplier() {
        }

        public SortedSetSupplier(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new SortedSetConverter(null) : new SortedSetConverter(delegate.get());
        }

        public Supplier<ParameterConverter> getDelegate() {
            return delegate;
        }

        public SortedSetSupplier setDelegate(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
            return this;
        }
    }

}
