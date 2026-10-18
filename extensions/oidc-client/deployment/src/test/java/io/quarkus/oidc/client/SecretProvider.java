package io.quarkus.oidc.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Named("vault-secret-provider")
public class SecretProvider implements CredentialsProvider {

    private final AtomicInteger namedClient1Counter = new AtomicInteger();
    private final AtomicInteger namedClient2Counter = new AtomicInteger();

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        Map<String, String> creds = new HashMap<>();

        if ("named-1-client-secret".equals(credentialsProviderName)) {
            boolean setCorrect = namedClient1Counter.incrementAndGet() == 2;
            creds.put("secret-from-vault", setCorrect ? "secret" : "wrong-secret");
        } else if ("named-2-client-secret".equals(credentialsProviderName)) {
            boolean setCorrect = namedClient2Counter.incrementAndGet() == 2;
            creds.put("secret-from-vault", setCorrect ? "secret" : "wrong-secret");
        } else {
            creds.put("secret-from-vault", "secret");
        }

        return creds;
    }

}
