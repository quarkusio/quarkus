package io.quarkus.runtime.configuration;

public class ClearSecret {
    private final String secret;

    ClearSecret(final String secret) {
        this.secret = secret;
    }

    public String get() {
        return secret;
    }

    public static ClearSecret of(final String secret) {
        return new ClearSecret(secret);
    }
}
