package io.quarkus.deployment.builditem;

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that can be used to unwrap CDI <s>or other proxies</s>.
 *
 * @deprecated Use {@code io.quarkus.arc.ClientProxy.unwrap(T)} instead.
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public final class ProxyUnwrapperBuildItem extends MultiBuildItem {

    private final Function<Object, Object> unwrapper;

    public ProxyUnwrapperBuildItem(Function<Object, Object> unwrapper) {
        this.unwrapper = unwrapper;
    }

    public Function<Object, Object> getUnwrapper() {
        return unwrapper;
    }
}
