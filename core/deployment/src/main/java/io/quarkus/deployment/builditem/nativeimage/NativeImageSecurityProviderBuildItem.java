package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a security provider should be included in the native image using
 * '-H:AdditionalSecurityProviders' option
 */
public final class NativeImageSecurityProviderBuildItem extends MultiBuildItem {
    private final String securityProvider;

    public NativeImageSecurityProviderBuildItem(String securityProvider) {
        this.securityProvider = securityProvider;
    }

    public String getSecurityProvider() {
        return securityProvider;
    }
}
