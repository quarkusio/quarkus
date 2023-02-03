package io.quarkus.it.rabbitmq;

import java.time.Instant;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Named("test-creds-provider")
public class TestCredentialsProvider implements CredentialsProvider {

    @ConfigProperty(name = "test-creds-provider.username")
    String username;

    @ConfigProperty(name = "test-creds-provider.password")
    String password;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        return Map.of(
                CredentialsProvider.USER_PROPERTY_NAME, username,
                CredentialsProvider.PASSWORD_PROPERTY_NAME, password,
                CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME, Instant.now().plusSeconds(90).toString());
    }
}
