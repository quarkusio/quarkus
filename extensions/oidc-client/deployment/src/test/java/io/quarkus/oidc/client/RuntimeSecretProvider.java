package io.quarkus.oidc.client;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.credentials.CredentialsProvider;

public class RuntimeSecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Map<String, String> creds = new HashMap<>();
        creds.put("secret-from-vault-for-jwt",
                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");
        return creds;
    }

}
