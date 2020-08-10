package io.quarkus.qrs.runtime.core.parameters.converters;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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

    public static class SetSupplier implements Supplier<ParameterConverter> {
        private Supplier<ParameterConverter> delegate;

        public SetSupplier() {
        }

        public SetSupplier(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ParameterConverter get() {
            return delegate == null ? new SetConverter(null) : new SetConverter(delegate.get());
        }

        public Supplier<ParameterConverter> getDelegate() {
            return delegate;
        }

        public SetSupplier setDelegate(Supplier<ParameterConverter> delegate) {
            this.delegate = delegate;
            return this;
        }
    }
}
