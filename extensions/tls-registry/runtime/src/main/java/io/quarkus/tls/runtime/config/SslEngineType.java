package io.quarkus.tls.runtime.config;

public enum SslEngineType {
    /**
     * Use the OpenSsl Engine, requires netty-tcnative.
     */
    OPENSSL,
    /**
     * Use the default JdkSsl Engine.
     */
    JDKSSL
}
