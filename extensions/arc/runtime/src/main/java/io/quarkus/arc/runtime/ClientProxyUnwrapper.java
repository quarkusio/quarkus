package io.quarkus.arc.runtime;

import java.util.function.Function;

import io.quarkus.arc.ClientProxy;

/**
 *
 * @deprecated See {@link ClientProxy}
 */
@Deprecated(since = "2.13", forRemoval = true)
public class ClientProxyUnwrapper implements Function<Object, Object> {
    @Override
    public Object apply(Object o) {
        return ClientProxy.unwrap(o);
    }
}
