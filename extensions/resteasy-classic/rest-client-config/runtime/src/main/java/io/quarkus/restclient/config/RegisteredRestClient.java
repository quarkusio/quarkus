package io.quarkus.restclient.config;

public class RegisteredRestClient {
    private final String fullName;
    private final String simpleName;
    private final String configKey;

    public RegisteredRestClient(final String fullName, final String simpleName) {
        this(fullName, simpleName, null);
    }

    public RegisteredRestClient(final String fullName, final String simpleName, final String configKey) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.configKey = configKey;
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
}
