package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;

public class PrivateMethodEntity {

    private String secret;
    private int code;

    public PrivateMethodEntity() {
    }

    public PrivateMethodEntity(String secret, int code) {
        this.secret = secret;
        this.code = code;
    }

    @GeneratedMethodAccessor
    private String getSecret() {
        return secret;
    }

    @GeneratedMethodAccessor
    private void setSecret(String secret) {
        this.secret = secret;
    }

    @GeneratedMethodAccessor
    private int getCode() {
        return code;
    }

    @GeneratedMethodAccessor
    private void setCode(int code) {
        this.code = code;
    }
}
