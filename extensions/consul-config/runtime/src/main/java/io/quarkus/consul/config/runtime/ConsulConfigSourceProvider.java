package io.quarkus.consul.config.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAwait;

class ConsulConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(ConsulConfigSourceProvider.class);

    private final ConsulConfig config;

    private final ConsulConfigGateway consulConfigGateway;
    private final ResponseConfigSourceUtil responseConfigSourceUtil;

    public ConsulConfigSourceProvider(ConsulConfig config) {
        this(config, new VertxConsulConfigGateway(config), new ResponseConfigSourceUtil());
    }

    // visible for testing
    ConsulConfigSourceProvider(ConsulConfig config, ConsulConfigGateway consulConfigGateway) {
        this(config, consulConfigGateway, new ResponseConfigSourceUtil());
    }

    private ConsulConfigSourceProvider(ConsulConfig config, ConsulConfigGateway consulConfigGateway,
            ResponseConfigSourceUtil responseConfigSourceUtil) {
        this.config = config;
        this.consulConfigGateway = consulConfigGateway;
        this.responseConfigSourceUtil = responseConfigSourceUtil;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader cl) {
        Map<String, ValueType> keys = config.keysAsMap();
        Map<String, ValueType> folders = config.foldersAsMap();
        if (keys.isEmpty() && folders.isEmpty()) {
            log.debug("No keys were configured for config source lookup");
            return Collections.emptyList();
        }

        List<ConfigSource> result = new ArrayList<>(keys.size());

        List<Uni<?>> allUnis = new ArrayList<>();

        for (Map.Entry<String, ValueType> entry : keys.entrySet()) {
            String fullKey = buildFullKey(entry);
            allUnis.add(consulConfigGateway.getValue(fullKey).invoke(new Consumer<Response>() {
                @Override
                public void accept(Response response) {
                    if (response != null) {
                        result.add(
                                responseConfigSourceUtil.toConfigSource(response, entry.getValue(),
                                        config.prefix));
                    } else {
                        String message = "Key '" + fullKey + "' not found in Consul.";
                        if (config.failOnMissingKey) {
                            throw new RuntimeException(message);
                        } else {
                            log.info(message);
                        }
                    }
                }
            }));
        }

        for (Map.Entry<String, ValueType> entry : folders.entrySet()) {
            String fullKey = buildFullKey(entry);
            allUnis.add(consulConfigGateway.getValueRecursive(fullKey).invoke(new Consumer<MultiResponse>() {
                @Override
                public void accept(MultiResponse response) {
                    if (response != null) {
                        response.getResponses().forEach(
                                r -> result.add(responseConfigSourceUtil.toConfigSource(r, entry.getValue(), config.prefix)));
                    } else {
                        String message = "Key '" + fullKey + "' not found in Consul.";
                        if (config.failOnMissingKey) {
                            throw new RuntimeException(message);
                        } else {
                            log.info(message);
                        }
                    }
                }
            }));
        }

        try {
            UniAwait<Void> await = Uni.combine().all().unis(allUnis).discardItems().await();
            if (config.agent.connectionTimeout.isZero() && config.agent.readTimeout.isZero()) {
                await.indefinitely();
            } else {
                await.atMost(config.agent.connectionTimeout.plus(config.agent.readTimeout.multipliedBy(2)));
            }
        } catch (CompletionException e) {
            throw new RuntimeException("An error occurred while attempting to fetch configuration from Consul.", e);
        } finally {
            consulConfigGateway.close();
        }

        return result;
    }

    private String buildFullKey(Map.Entry<String, ValueType> entry) {
        return config.prefix.map(s -> s + "/" + entry.getKey()).orElseGet(entry::getKey);
    }
}
