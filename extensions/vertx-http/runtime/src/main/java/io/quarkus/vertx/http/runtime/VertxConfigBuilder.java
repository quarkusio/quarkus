package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class VertxConfigBuilder implements ConfigBuilder {
    private static final String QUARKUS_HTTP_HOST = "quarkus.http.host";
    private static final String ALL_INTERFACES = "0.0.0.0";

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        // It may have been recorded, so only set if it not available in the defaults
        if (builder.getDefaultValues().get(QUARKUS_HTTP_HOST) == null) {
            // Sets the default host config value, depending on the launch mode
            if (LaunchMode.isRemoteDev()) {
                // in remote-dev mode we need to listen on all interfaces
                builder.withDefaultValue(QUARKUS_HTTP_HOST, ALL_INTERFACES);
            } else {
                // In dev-mode we want to only listen on localhost so others on the network cannot connect to the application.
                // However, in WSL this would result in the application not being accessible,
                // so in that case, we launch it on all interfaces.
                builder.withDefaultValue(QUARKUS_HTTP_HOST,
                        (LaunchMode.current().isDevOrTest() && !isWSL()) ? "localhost" : ALL_INTERFACES);
            }
        }
        return builder;
    }

    /**
     * @return {@code true} if the application is running in a WSL (Windows Subsystem for Linux) environment
     */
    private boolean isWSL() {
        var sysEnv = System.getenv();
        return sysEnv.containsKey("IS_WSL") || sysEnv.containsKey("WSL_DISTRO_NAME");
    }
}
