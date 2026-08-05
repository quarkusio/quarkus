package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class IgnoredType {

    private String secret;

    public IgnoredType() {
    }

    public IgnoredType(String secret) {
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
