package io.quarkus.consul.config.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

class ConsulConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(ConsulConfigSourceProvider.class);

    private final ConsulConfig config;

    private final ConsulConfigGateway consulConfigGateway;
    private final ResponseConfigSourceUtil responseConfigSourceUtil;

    public ConsulConfigSourceProvider(ConsulConfig config) {
        this(config, new DefaultConsulConfigGateway(config), new ResponseConfigSourceUtil());
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
        if (keys.isEmpty()) {
            log.debug("No keys were configured for config source lookup");
            return Collections.emptyList();
        }

        List<ConfigSource> result = new ArrayList<>(keys.size());

        for (Map.Entry<String, ValueType> entry : keys.entrySet()) {
            String fullKey = config.prefix.isPresent() ? config.prefix.get() + "/" + entry.getKey() : entry.getKey();
            log.debug("Attempting to look up value of key '" + fullKey + "' from Consul.");

            try {
                Optional<Response> optionalResponse = consulConfigGateway.getValue(fullKey);
                if (optionalResponse.isPresent()) {
                    result.add(
                            responseConfigSourceUtil.toConfigSource(optionalResponse.get(), entry.getValue(), config.prefix));
                } else {
                    String message = "Key '" + fullKey + "' not found in Consul.";
                    if (config.failOnMissingKey) {
                        throw new RuntimeException(message);
                    } else {
                        log.info(message);
                    }
                }
                log.debug("Done reading value of key '" + fullKey + "'");
            } catch (IOException e) {
                throw new UncheckedIOException("An error occurred while attempting to fetch configuration from Consul.", e);
            }
        }

        return result;
    }
}
