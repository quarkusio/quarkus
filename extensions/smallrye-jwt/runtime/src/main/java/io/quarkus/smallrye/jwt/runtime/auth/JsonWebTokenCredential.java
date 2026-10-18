package io.quarkus.smallrye.jwt.runtime.auth;

import io.quarkus.security.credential.TokenCredential;

public class JsonWebTokenCredential extends TokenCredential {

    public JsonWebTokenCredential() {
        this(null);
    }

    public JsonWebTokenCredential(String token) {
        super(token, "bearer");
    }
}
