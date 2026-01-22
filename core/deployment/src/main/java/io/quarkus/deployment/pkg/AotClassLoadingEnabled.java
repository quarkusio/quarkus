package io.quarkus.deployment.pkg;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.LaunchMode;

public class AotClassLoadingEnabled implements BooleanSupplier {

    private final PackageConfig packageConfig;
    private final LaunchMode launchMode;

    AotClassLoadingEnabled(PackageConfig packageConfig, LaunchMode launchMode) {
        this.packageConfig = packageConfig;
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        if (launchMode != LaunchMode.NORMAL) {
            return false;
        }

        return packageConfig.jar().enabled() &&
                packageConfig.jar().appcds().enabled() &&
                packageConfig.jar().appcds().useAot();
    }
}
