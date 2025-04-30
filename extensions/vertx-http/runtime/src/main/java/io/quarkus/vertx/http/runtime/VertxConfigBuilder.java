package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class VertxConfigBuilder implements ConfigBuilder {
    private static final String QUARKUS_HTTP_HOST = "quarkus.http.host";
    private static final String QUARKUS_MANAGEMENT_HOST = "quarkus.management.host";

    private static final String LOCALHOST = "localhost";
    private static final String ALL_INTERFACES = "0.0.0.0";

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        defineHttpInterface(builder, QUARKUS_HTTP_HOST);
        defineHttpInterface(builder, QUARKUS_MANAGEMENT_HOST);
        return builder;
    }

    private static void defineHttpInterface(SmallRyeConfigBuilder builder, String configurationProperty) {
        // It may have been recorded, so only set if it not available in the defaults
        if (builder.getDefaultValues().get(configurationProperty) != null) {
            return;
        }

        // Sets the default host config value, depending on the launch mode
        if (LaunchMode.isRemoteDev()) {
            // in remote dev mode, we want to listen on all interfaces
            // to make sure the application is accessible
            builder.withDefaultValue(configurationProperty, ALL_INTERFACES);
        } else if (LaunchMode.current().isDevOrTest()) {
            if (!isWSL()) {
                // in dev mode, we want to listen only on localhost
                // to make sure the app is not accessible from the outside
                builder.withDefaultValue(configurationProperty, LOCALHOST);
            } else {
                // except when using WSL, as otherwise the app wouldn't be accessible from the host
                builder.withDefaultValue(configurationProperty, ALL_INTERFACES);
            }
        } else {
            // in all the other cases, we make sure the app is accessible on all the interfaces by default
            builder.withDefaultValue(configurationProperty, ALL_INTERFACES);
        }
    }

    /**
     * @return {@code true} if the application is running in a WSL (Windows Subsystem for Linux) environment
     */
    private static boolean isWSL() {
        var sysEnv = System.getenv();
        return sysEnv.containsKey("IS_WSL") || sysEnv.containsKey("WSL_DISTRO_NAME");
    }
}
