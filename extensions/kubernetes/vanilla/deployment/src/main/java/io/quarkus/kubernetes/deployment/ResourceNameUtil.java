package io.quarkus.kubernetes.deployment;

import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public final class ResourceNameUtil {

    private ResourceNameUtil() {
    }

    /**
     * Provides the name of a generated Kubernetes resource.
     * Uses the value from the configuration object if it exists, otherwise falls back to
     * the application name
     */
    public static String getResourceName(PlatformConfiguration platformConfiguration,
            ApplicationInfoBuildItem applicationInfo) {
        return platformConfiguration.getName().orElse(applicationInfo.getName()).trim();
    }
}
