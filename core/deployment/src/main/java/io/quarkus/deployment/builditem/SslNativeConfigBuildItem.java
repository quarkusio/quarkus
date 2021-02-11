package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SslNativeConfigBuildItem extends SimpleBuildItem {

    private Optional<Boolean> enableSslNativeConfig;

    public SslNativeConfigBuildItem(Optional<Boolean> enableSslNativeConfig) {
        this.enableSslNativeConfig = enableSslNativeConfig;
    }

    public Optional<Boolean> get() {
        return enableSslNativeConfig;
    }

    public boolean isEnabled() {
        // default is to disable the SSL native support
        return enableSslNativeConfig.isPresent() && enableSslNativeConfig.get();
    }

    public boolean isExplicitlyDisabled() {
        return enableSslNativeConfig.isPresent() && !enableSslNativeConfig.get();
    }
}
