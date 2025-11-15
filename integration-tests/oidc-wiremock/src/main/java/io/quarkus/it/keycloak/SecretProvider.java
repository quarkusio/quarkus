package io.quarkus.it.keycloak;

import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Named("vault-secret-provider")
public class SecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        return Collections.singletonMap("secret-from-vault",
                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");
    }

}
