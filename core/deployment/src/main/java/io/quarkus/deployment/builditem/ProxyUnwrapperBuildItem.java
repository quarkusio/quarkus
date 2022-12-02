package io.quarkus.deployment.builditem;

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that can be used to unwrap CDI or other proxies
 */
public final class ProxyUnwrapperBuildItem extends MultiBuildItem {

    private final Function<Object, Object> unwrapper;

    public ProxyUnwrapperBuildItem(Function<Object, Object> unwrapper) {
        this.unwrapper = unwrapper;
    }

    public Function<Object, Object> getUnwrapper() {
        return unwrapper;
    }
}
