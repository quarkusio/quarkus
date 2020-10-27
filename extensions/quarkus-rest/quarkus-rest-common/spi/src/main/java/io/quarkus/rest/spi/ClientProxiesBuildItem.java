package io.quarkus.rest.spi;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.client.WebTarget;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class ClientProxiesBuildItem extends SimpleBuildItem {

    final Map<String, RuntimeValue<Function<WebTarget, ?>>> clientProxies;

    public ClientProxiesBuildItem(Map<String, RuntimeValue<Function<WebTarget, ?>>> clientProxies) {
        this.clientProxies = clientProxies;
    }

    public Map<String, RuntimeValue<Function<WebTarget, ?>>> getClientProxies() {
        return clientProxies;
    }
}
