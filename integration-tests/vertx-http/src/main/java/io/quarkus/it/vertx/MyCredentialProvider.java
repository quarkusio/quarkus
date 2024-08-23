package io.quarkus.it.vertx;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
public class MyCredentialProvider implements CredentialsProvider {

    private final HashMap<String, Map<String, String>> credentials;

    public MyCredentialProvider() {
        this.credentials = new HashMap<>();
        this.credentials.put("http-credentials", Map.of("alias-password", "serverpw"));
    }

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        return credentials.get(credentialsProviderName);
    }
}
