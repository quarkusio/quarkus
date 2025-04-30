package io.quarkus.reactive.mssql.client;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
public class CustomCredentialsProvider implements CredentialsProvider {

    private static final Logger log = Logger.getLogger(CustomCredentialsProvider.class.getName());

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Map<String, String> properties = new HashMap<>();
        properties.put(USER_PROPERTY_NAME, "sa");
        properties.put(PASSWORD_PROPERTY_NAME, "yourStrong(!)Password");
        log.info("credentials provider returning " + properties);
        return properties;
    }
}
