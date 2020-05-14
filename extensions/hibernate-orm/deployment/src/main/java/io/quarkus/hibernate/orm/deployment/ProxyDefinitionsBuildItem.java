package io.quarkus.hibernate.orm.deployment;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;

public final class ProxyDefinitionsBuildItem extends SimpleBuildItem {

    private final PreGeneratedProxies proxies;

    public ProxyDefinitionsBuildItem(PreGeneratedProxies proxies) {
        Objects.requireNonNull(proxies);
        this.proxies = proxies;
    }

    public PreGeneratedProxies getProxies() {
        return proxies;
    }
}
