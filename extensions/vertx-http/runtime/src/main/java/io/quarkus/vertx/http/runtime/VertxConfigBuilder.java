package io.quarkus.vertx.http.runtime;

import static io.quarkus.runtime.LaunchMode.TEST;
import static java.lang.Integer.MAX_VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.quarkus.runtime.configuration.RandomPorts;
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class VertxConfigBuilder implements ConfigBuilder {
    private static final String QUARKUS_HTTP_HOST = "quarkus.http.host";

    private static final String LOCALHOST = "localhost";
    private static final String ALL_INTERFACES = "0.0.0.0";

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        // It may have been recorded, so only set if it not available in the defaults
        if (builder.getDefaultValues().get(QUARKUS_HTTP_HOST) == null) {
            // Sets the default host config value, depending on the launch mode
            if (LaunchMode.isRemoteDev()) {
                // in remote dev mode, we want to listen on all interfaces
                // to make sure the application is accessible
                builder.withDefaultValue(QUARKUS_HTTP_HOST, ALL_INTERFACES);
            } else if (LaunchMode.current().isDevOrTest()) {
                if (!isWSL()) {
                    // in dev mode, we want to listen only on localhost
                    // to make sure the app is not accessible from the outside
                    builder.withDefaultValue(QUARKUS_HTTP_HOST, LOCALHOST);
                } else {
                    // except when using WSL, as otherwise the app wouldn't be accessible from the host
                    builder.withDefaultValue(QUARKUS_HTTP_HOST, ALL_INTERFACES);
                }
            } else {
                // in all the other cases, we make sure the app is accessible on all the interfaces by default
                builder.withDefaultValue(QUARKUS_HTTP_HOST, ALL_INTERFACES);
            }
        }
        builder.withSources(new RandomPortConfigSourceFactory());
        return builder;
    }

    /**
     * @return {@code true} if the application is running in a WSL (Windows Subsystem for Linux) environment
     */
    private boolean isWSL() {
        var sysEnv = System.getenv();
        return sysEnv.containsKey("IS_WSL") || sysEnv.containsKey("WSL_DISTRO_NAME");
    }

    private static class RandomPortConfigSourceFactory implements ConfigSourceFactory {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
            Map<String, String> randomPorts = new HashMap<>();

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withSources(context.getConfigSources())
                    .withMapping(HttpConfig.class)
                    .withMapping(ManagementConfig.class)
                    .build();

            HttpConfig httpConfig = config.getConfigMapping(HttpConfig.class);
            if (httpConfig.hostEnabled()) {
                String host = httpConfig.testHost().orElse(httpConfig.host());

                int port = httpConfig.determinePort(LaunchMode.current());
                if (port < 0) {
                    String randomPort = RandomPorts.get(host, port) + "";
                    randomPorts.put("quarkus.http.port", randomPort);
                    if (LaunchMode.current().equals(TEST)) {
                        randomPorts.put("quarkus.http.test-port", randomPort);
                    }
                }

                int sslPort = httpConfig.determineSslPort(LaunchMode.current());
                if (sslPort < 0) {
                    String randomPort = RandomPorts.get(host, sslPort) + "";
                    randomPorts.put("quarkus.http.ssl-port", randomPort);
                    if (LaunchMode.current().equals(TEST)) {
                        randomPorts.put("quarkus.http.test-ssl-port", randomPort);
                    }
                }
            }

            ManagementConfig managementConfig = config.getConfigMapping(ManagementConfig.class);
            if (managementConfig.hostEnabled()) {
                String host = managementConfig.host();

                int port = managementConfig.determinePort(LaunchMode.current());
                if (port < 0) {
                    String randomPort = RandomPorts.get(host, port) + "";
                    randomPorts.put("quarkus.management.port", randomPort);
                    if (LaunchMode.current().equals(TEST)) {
                        randomPorts.put("quarkus.management.test-port", randomPort);
                    }
                }
            }

            return randomPorts.isEmpty() ? Collections.emptyList()
                    : List.of(new MapBackedConfigSource("Quarkus HTTP Random Ports", randomPorts, MAX_VALUE - 1000) {
                    });
        }
    }
}
