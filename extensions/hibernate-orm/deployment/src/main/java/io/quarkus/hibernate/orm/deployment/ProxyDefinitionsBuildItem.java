package io.quarkus.hibernate.orm.deployment;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;

/**
 * Contains the reference to the class definitions of the proxies
 * that Hibernate ORM might require at runtime.
 * In Quarkus such proxies are built upfront, during the build.
 * This needs to be a separate build item from other components so
 * to avoid cycles in the rather complex build graph required by
 * this extension.
 */
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
