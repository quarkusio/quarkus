package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item is produced once the effective service account used for the generated resources is computed. Useful for
 * downstream
 * extensions that need to know this information to wait until it is made available.
 */
public final class KubernetesEffectiveServiceAccountBuildItem extends MultiBuildItem {
    private final String serviceAccountName;
    private final String namespace;

    public KubernetesEffectiveServiceAccountBuildItem(String serviceAccountName, String namespace) {
        this.serviceAccountName = serviceAccountName;
        this.namespace = namespace;
    }

    /**
     * The effective service account name after all the build time configuration has taken effect
     *
     * @return the effective service account name as used in the generated manifests for the main application deployment
     */
    public String getServiceAccountName() {
        return serviceAccountName;
    }

    /**
     * The effective service account namespace
     *
     * @return the effective service account name as used in the generated manifests for the main application deployment
     */
    public String getNamespace() {
        return namespace;
    }
}
