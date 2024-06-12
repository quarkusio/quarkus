
package io.quarkus.kubernetes.spi;

import java.util.Collection;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesAdditionalResourceBuildItem extends MultiBuildItem {

    private final String name;

    public KubernetesAdditionalResourceBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static boolean hasItem(String name, Collection<KubernetesAdditionalResourceBuildItem> items) {
        return items.stream().filter(i -> name.equals(i.name)).findAny().isPresent();
    }
}
