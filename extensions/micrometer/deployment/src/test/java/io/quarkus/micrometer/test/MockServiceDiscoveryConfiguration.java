package io.quarkus.micrometer.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the {@code MockServiceDiscoveryProvider} ServiceDiscovery.
 */
public class MockServiceDiscoveryConfiguration implements io.smallrye.stork.api.config.ConfigWithType {
    private final Map<String, String> parameters;

    /**
     * Creates a new MockConfiguration
     *
     * @param params
     *        the parameters, must not be {@code null}
     */
    public MockServiceDiscoveryConfiguration(Map<String, String> params) {
        parameters = Collections.unmodifiableMap(params);
    }

    /**
     * Creates a new MockConfiguration
     */
    public MockServiceDiscoveryConfiguration() {
        parameters = Collections.emptyMap();
    }

    /**
     * @return the type
     */
    @Override
    public String type() {
        return "mock";
    }

    /**
     * @return the parameters
     */
    @Override
    public Map<String, String> parameters() {
        return parameters;
    }

    private MockServiceDiscoveryConfiguration extend(String key, String value) {
        Map<String, String> copy = new HashMap<>(parameters);
        copy.put(key, value);
        return new MockServiceDiscoveryConfiguration(copy);
    }
}
