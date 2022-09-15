package io.quarkus.it.bouncycastle;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Unremovable
public class SecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Map<String, String> creds = new HashMap<>();
        creds.put("keystore-password", "password");
        creds.put("truststore-password", "password");
        return creds;
    }

}
