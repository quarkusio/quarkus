package io.quarkus.oidc.test;

import java.util.Collections;
import java.util.Map;

import io.quarkus.credentials.CredentialsProvider;

public class RuntimeSecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        if ("oidc".equals(credentialsProviderName)) {
            return Collections.singletonMap("runtime-secret-from-vault", "secret");
        } else {
            return Map.of();
        }
    }

}
