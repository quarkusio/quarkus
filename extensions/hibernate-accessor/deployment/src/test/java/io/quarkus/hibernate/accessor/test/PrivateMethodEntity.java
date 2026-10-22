package io.quarkus.hibernate.accessor.test;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class PrivateMethodEntity {

    private String secret;
    private int code;

    public PrivateMethodEntity() {
    }

    public PrivateMethodEntity(String secret, int code) {
        this.secret = secret;
        this.code = code;
    }

    @ReflectionFreeAccessor
    private String getSecret() {
        return secret;
    }

    @ReflectionFreeAccessor
    private void setSecret(String secret) {
        this.secret = secret;
    }

    @ReflectionFreeAccessor
    private int getCode() {
        return code;
    }

    @ReflectionFreeAccessor
    private void setCode(int code) {
        this.code = code;
    }
}
