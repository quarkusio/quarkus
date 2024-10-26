package io.quarkus.restclient.config;

import io.smallrye.config.NameIterator;

public class RegisteredRestClient {
    private final String fullName;
    private final String simpleName;
    private final String configKey;
    private final boolean configKeyComposed;

    public RegisteredRestClient(final String fullName, final String simpleName) {
        this(fullName, simpleName, null);
    }

    public RegisteredRestClient(final Class<?> client, final String configKey) {
        this(client.getName(), client.getSimpleName(), configKey);
    }

    public RegisteredRestClient(final String fullName, final String simpleName, final String configKey) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.configKey = configKey;
        this.configKeyComposed = configKey != null && new NameIterator(configKey).nextSegmentEquals(configKey);
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

    public boolean isConfigKeyEqualsNames() {
        if (configKey == null) {
            return false;
        }

        return configKey.equals(fullName) || configKey.equals(simpleName);
    }

    public boolean isConfigKeyComposed() {
        if (configKey == null) {
            throw new IllegalStateException("configKey is null");
        }
        return !configKeyComposed;
    }
}
