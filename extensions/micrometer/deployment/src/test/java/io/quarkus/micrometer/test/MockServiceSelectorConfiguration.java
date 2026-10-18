package io.quarkus.micrometer.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the {@code MockLoadBalancerProvider} LoadBalancer.
 */
public class MockServiceSelectorConfiguration implements io.smallrye.stork.api.config.ConfigWithType {
    private final Map<String, String> parameters;

    /**
     * Creates a new FakeSelectorConfiguration
     *
     * @param params the parameters, must not be {@code null}
     */
    public MockServiceSelectorConfiguration(Map<String, String> params) {
        parameters = Collections.unmodifiableMap(params);
    }

    /**
     * Creates a new FakeSelectorConfiguration
     */
    public MockServiceSelectorConfiguration() {
        parameters = Collections.emptyMap();
    }

    /**
     * @return the type
     */
    @Override
    public String type() {
        return "fake-selector";
    }

    /**
     * @return the parameters
     */
    @Override
    public Map<String, String> parameters() {
        return parameters;
    }

    private MockServiceSelectorConfiguration extend(String key, String value) {
        Map<String, String> copy = new HashMap<>(parameters);
        copy.put(key, value);
        return new MockServiceSelectorConfiguration(copy);
    }
}
