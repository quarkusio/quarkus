package io.quarkus.grpc.runtime.stork;

import java.util.Collections;
import java.util.Map;

import io.smallrye.stork.api.config.ConfigWithType;

/**
 * Configuration for the {@code flaky} test {@link ServiceDiscovery} provider.
 */
final class FlakyServiceDiscoveryConfiguration implements ConfigWithType {

    private final Map<String, String> parameters;

    FlakyServiceDiscoveryConfiguration(Map<String, String> parameters) {
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    String getAddressList() {
        return parameters.get("address-list");
    }

    long getFailureDelayMs() {
        String delay = parameters.get("failure-delay-ms");
        return delay == null ? 0L : Long.parseLong(delay.trim());
    }

    boolean isEmptyOnFirstLookup() {
        return Boolean.parseBoolean(parameters.get("empty-on-first-lookup"));
    }

    @Override
    public String type() {
        return "flaky";
    }

    @Override
    public Map<String, String> parameters() {
        return parameters;
    }
}
