package io.quarkus.devservices.deployment;

import java.util.function.BooleanSupplier;

public class IsRuntimeModuleAvailable implements BooleanSupplier {
    static final String IO_QUARKUS_DEVSERVICES_CONFIG_BUILDER_CLASS = "io.quarkus.devservice.runtime.config.DevServicesConfigBuilder";
    static final boolean CONFIG_BUILDER_AVAILABLE = isClassAvailable(
            IO_QUARKUS_DEVSERVICES_CONFIG_BUILDER_CLASS);

    @Override
    public boolean getAsBoolean() {
        return CONFIG_BUILDER_AVAILABLE;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
