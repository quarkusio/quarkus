package io.quarkus.mongodb.deployment;

import javax.net.ssl.SSLContext;

import io.quarkus.mongodb.runtime.SSLContextConfig;

public class InvalidCustomSSLContext implements SSLContextConfig {

    private InvalidCustomSSLContext() {

    }

    @Override
    public SSLContext getSSLContext() {
        return null;
    }
}
