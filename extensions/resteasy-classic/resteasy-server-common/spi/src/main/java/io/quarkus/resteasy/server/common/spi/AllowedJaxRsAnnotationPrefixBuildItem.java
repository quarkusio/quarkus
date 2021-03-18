package io.quarkus.resteasy.server.common.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * The package prefix of any annotations that have to be compatible with JaxRs resource class
 * to allow constructor injection.
 */
public final class AllowedJaxRsAnnotationPrefixBuildItem extends MultiBuildItem {

    private final String prefix;

    public AllowedJaxRsAnnotationPrefixBuildItem(String prefix) {
        this.prefix = prefix;
    }

    public String getAnnotationPrefix() {
        return prefix;
    }
}
