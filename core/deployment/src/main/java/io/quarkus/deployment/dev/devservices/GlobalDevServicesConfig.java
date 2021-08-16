package io.quarkus.deployment.dev.devservices;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "devservices")
public class GlobalDevServicesConfig {

    /**
     * Global flag that can be used to disable all Dev Services. If this is set to false then Dev Services will not be used.
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

    public static class Enabled implements BooleanSupplier {

        final GlobalDevServicesConfig config;

        public Enabled(GlobalDevServicesConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
