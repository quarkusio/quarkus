package io.quarkus.reactive.datasource;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;

public abstract class ChangingCredentialsProviderBase implements CredentialsProvider {

    private static final Logger log = Logger.getLogger(ChangingCredentialsProviderBase.class.getName());

    private final String user2;
    private final String password2;

    private volatile Map<String, String> properties;

    protected ChangingCredentialsProviderBase(String user1, String password1, String user2, String password2) {
        properties = new HashMap<>();
        properties.put(USER_PROPERTY_NAME, user1);
        properties.put(PASSWORD_PROPERTY_NAME, password1);
        this.user2 = user2;
        this.password2 = password2;
    }

    public void changeProperties() {
        properties = new HashMap<>();
        properties.put(USER_PROPERTY_NAME, user2);
        properties.put(PASSWORD_PROPERTY_NAME, password2);
    }

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        log.info("credentials provider returning " + properties);
        return properties;
    }
}
