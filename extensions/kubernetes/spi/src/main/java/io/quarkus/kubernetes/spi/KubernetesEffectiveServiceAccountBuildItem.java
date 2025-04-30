package io.quarkus.kubernetes.spi;

/**
 * This build item is produced once the effective service account used for the generated resources is computed. Useful for
 * downstream
 * extensions that need to know this information to wait until it is made available.
 */
public final class KubernetesEffectiveServiceAccountBuildItem extends BaseTargetable {
    private final String serviceAccountName;
    private final String namespace;
    private final boolean wasSet;

    public KubernetesEffectiveServiceAccountBuildItem(String serviceAccountName, String namespace, boolean wasSet) {
        this(serviceAccountName, namespace, wasSet, null);
    }

    public KubernetesEffectiveServiceAccountBuildItem(String serviceAccountName, String namespace, boolean wasSet,
            String target) {
        super(target);
        this.serviceAccountName = serviceAccountName;
        this.namespace = namespace;
        this.wasSet = wasSet;
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

    /**
     * Determines whether the service account name is automatically derived from the application name as opposed to explicitly
     * set by the user
     *
     * @return {@code true} if the service account is automatically derived from the application name, {@code false} if
     *         explicitly set by the user
     */
    public boolean wasSet() {
        return wasSet;
    }
}
