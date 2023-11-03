package io.quarkus.oidc;

import io.quarkus.security.credential.TokenCredential;

public class IdTokenCredential extends TokenCredential {
    private final boolean internal;

    public IdTokenCredential() {
        this(null);
    }

    public IdTokenCredential(String token) {
        this(token, false);
    }

    public IdTokenCredential(String token, boolean internal) {
        super(token, "id_token");
        this.internal = internal;
    }

    public boolean isInternal() {
        return internal;
    }
}
