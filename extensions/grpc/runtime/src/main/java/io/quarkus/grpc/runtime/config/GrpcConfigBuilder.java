package io.quarkus.grpc.runtime.config;

import static io.quarkus.runtime.LaunchMode.TEST;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.grpc.runtime.GrpcTestPortUtils;
import io.quarkus.grpc.runtime.config.GrpcConfig.GrpcClient;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigBuilder;
import io.quarkus.runtime.configuration.RandomPorts;
import io.quarkus.vertx.http.runtime.HttpConfig;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

public class GrpcConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new RandomPortConfigSourceFactory());
    }

    private static class RandomPortConfigSourceFactory implements ConfigSourceFactory {
        @Override
        public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
            Map<String, String> randomPorts = new HashMap<>();

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                    .withSources(context.getConfigSources())
                    .withMapping(HttpConfig.class)
                    .withMapping(GrpcConfig.class)
                    .build();

            GrpcConfig grpcConfig = config.getConfigMapping(GrpcConfig.class);

            int port = grpcConfig.server().determinePort(LaunchMode.current());
            if (port <= 0) {
                String randomPort = RandomPorts.get(grpcConfig.server().host(), port) + "";
                randomPorts.put("quarkus.grpc.server.port", randomPort);
                if (LaunchMode.current().equals(TEST)) {
                    randomPorts.put("quarkus.grpc.server.test-port", randomPort);
                }
            }

            HttpConfig httpConfig = config.getConfigMapping(HttpConfig.class);
            for (Map.Entry<String, GrpcClient> client : grpcConfig.clients().entrySet()) {
                int clientPort = client.getValue().determinePort(LaunchMode.current(),
                        testPort(grpcConfig.server(), httpConfig));
                if (clientPort <= 0) {
                    String randomPort = RandomPorts.get(client.getValue().host(), clientPort) + "";
                    randomPorts.put(client.getValue().portName().getName(), randomPort);
                    if (LaunchMode.current().equals(TEST)) {
                        randomPorts.put(client.getValue().testPortName().getName(), randomPort);
                    }
                }
            }

            return randomPorts.isEmpty() ? emptyList()
                    : List.of(new MapBackedConfigSource("Quarkus GRPC Random Ports", randomPorts, MAX_VALUE - 1000) {
                    });
        }
    }

    /**
     * Mostly a copy of {@link GrpcTestPortUtils#testPort(GrpcServerConfiguration)}, with some changes, because it
     * seems that the original implementation returns the same value in different if branches.
     * <p>
     * Still, we should validate if this is really what we want to do.
     */
    private static int testPort(GrpcConfig.GrpcServer server, HttpConfig httpConfig) {
        if (server.useSeparateServer()) {
            return server.testPort();
        }

        if (!server.ssl().isEmpty() || !server.plainText()) {
            return httpConfig.determineSslPort(TEST);
        }

        return httpConfig.determinePort(TEST);
    }
}
