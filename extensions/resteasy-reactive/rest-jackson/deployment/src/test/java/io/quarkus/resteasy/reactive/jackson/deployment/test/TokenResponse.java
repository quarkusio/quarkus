package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {

    private final String accessToken;
    private final Integer expiresIn;

    public TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") Integer expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }
}