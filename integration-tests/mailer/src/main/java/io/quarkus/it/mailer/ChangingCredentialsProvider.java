package io.quarkus.it.mailer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;

@ApplicationScoped
@Unremovable
public class ChangingCredentialsProvider implements CredentialsProvider {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        int count = counter.getAndIncrement();
        return Map.of(USER_PROPERTY_NAME, "user" + count, PASSWORD_PROPERTY_NAME, "pass" + count);
    }

    public int count() {
        return counter.get();
    }
}
