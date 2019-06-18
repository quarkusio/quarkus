package io.quarkus.arc.runtime;

import java.util.function.Function;

import io.quarkus.arc.ClientProxy;

public class ClientProxyUnwrapper implements Function<Object, Object> {
    @Override
    public Object apply(Object o) {
        if (o instanceof ClientProxy) {
            return ((ClientProxy) o).arc_contextualInstance();
        }
        return o;
    }
}
