package io.quarkus.oidc.client;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Unremovable
@Named("vault-secret-provider")
public class SecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Map<String, String> creds = new HashMap<>();
        creds.put("secret-from-vault", "secret");
        creds.put("secret-from-vault-for-jwt",
                "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");
        return creds;
    }

}
