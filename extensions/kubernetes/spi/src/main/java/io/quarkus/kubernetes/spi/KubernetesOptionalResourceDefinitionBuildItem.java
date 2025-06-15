
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that extension can use to mark a specific resource definition as optional. An optional resource, when
 * fails to get deployed, will not cause the entire deployment process to fail, but will log a warning instead.
 */
public final class KubernetesOptionalResourceDefinitionBuildItem extends MultiBuildItem {

    private final String apiVersion;
    private final String kind;

    public KubernetesOptionalResourceDefinitionBuildItem(String apiVersion, String kind) {
        this.apiVersion = apiVersion;
        this.kind = kind;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }
}
