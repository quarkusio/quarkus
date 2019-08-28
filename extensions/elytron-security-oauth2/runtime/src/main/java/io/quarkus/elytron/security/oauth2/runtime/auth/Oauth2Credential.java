package io.quarkus.elytron.security.oauth2.runtime.auth;

import io.quarkus.elytron.security.runtime.UndertowTokenCredential;

public class Oauth2Credential implements UndertowTokenCredential {
    private String bearerToken;

    public Oauth2Credential(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBearerToken() {
        return bearerToken;
    }

}
