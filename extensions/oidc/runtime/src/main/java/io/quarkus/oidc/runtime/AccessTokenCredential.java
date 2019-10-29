package io.quarkus.oidc.runtime;

import io.quarkus.security.credential.TokenCredential;

public class AccessTokenCredential extends TokenCredential {

    public AccessTokenCredential(String token) {
        super(token, "bearer");
    }
}
