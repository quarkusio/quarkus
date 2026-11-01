package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

public class PrivateMethodEntity {

    private String secret;
    private int code;

    public PrivateMethodEntity() {
    }

    public PrivateMethodEntity(String secret, int code) {
        this.secret = secret;
        this.code = code;
    }

    @ReflectionFreeMethodAccessor
    private String getSecret() {
        return secret;
    }

    @ReflectionFreeMethodAccessor
    private void setSecret(String secret) {
        this.secret = secret;
    }

    @ReflectionFreeMethodAccessor
    private int getCode() {
        return code;
    }

    @ReflectionFreeMethodAccessor
    private void setCode(int code) {
        this.code = code;
    }
}
