package io.quarkus.tls.runtime.config;

public enum SslEngineType {
    /**
     * Use the OpenSSL engine, requires netty-tcnative.
     */
    OPENSSL,
    /**
     * Use the default JDK SSL engine.
     */
    JDKSSL
}
