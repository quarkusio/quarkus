package io.quarkus.oidc.test;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Named("vault-secret-provider")
public class SecretProvider implements CredentialsProvider {

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        return Collections.singletonMap("secret-from-vault", "secret");
    }

}
