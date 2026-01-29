package io.quarkus.deployment.pkg;

import java.util.function.BooleanSupplier;

public class AotClassLoadingEnabled implements BooleanSupplier {

    private final PackageConfig packageConfig;

    AotClassLoadingEnabled(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.jar().enabled() &&
                packageConfig.jar().appcds().enabled() &&
                packageConfig.jar().appcds().useAot();
    }
}
