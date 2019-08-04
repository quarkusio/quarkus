package io.quarkus.elytron.security.oauth2.runtime.auth;

import io.undertow.security.idm.Credential;

public class Oauth2Credential implements Credential {
    private String bearerToken;

    public Oauth2Credential(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBearerToken() {
        return bearerToken;
    }

}
