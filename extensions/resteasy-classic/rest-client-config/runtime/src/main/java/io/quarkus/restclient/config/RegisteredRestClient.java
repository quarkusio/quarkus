package io.quarkus.restclient.config;

import io.smallrye.config.NameIterator;

public class RegisteredRestClient {
    private final String fullName;
    private final String simpleName;
    private final String configKey;
    private final boolean configKeySegments;

    public RegisteredRestClient(final String fullName, final String simpleName) {
        this(fullName, simpleName, null);
    }

    public RegisteredRestClient(final String fullName, final String simpleName, final String configKey) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.configKey = configKey;
        this.configKeySegments = configKey != null && new NameIterator(configKey).nextSegmentEquals(configKey);
    }

    public String getFullName() {
        return fullName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public boolean isConfigKeySegments() {
        if (configKey == null) {
            throw new IllegalStateException("configKey is null");
        }
        return !configKeySegments;
    }
}
