package io.quarkus.redis.runtime.client.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface TlsConfig {

    /**
     * Whether SSL/TLS is enabled.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Enable trusting all certificates. Disabled by default.
     */
    @WithDefault("false")
    boolean trustAll();

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pfx} must be disabled.
     */
    PemTrustCertConfiguration trustCertificatePem();

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@code #trust-certificate-pem} and {@code #trust-certificate-pfx} must be disabled.
     */
    JksConfiguration trustCertificateJks();

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pem} must be disabled.
     */
    PfxConfiguration trustCertificatePfx();

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pfx} must be disabled.
     */
    PemKeyCertConfiguration keyCertificatePem();

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@code #key-certificate-pem} and {@code #key-certificate-pfx} must be disabled.
     */
    JksConfiguration keyCertificateJks();

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pem} must be disabled.
     */
    PfxConfiguration keyCertificatePfx();

    /**
     * The hostname verification algorithm to use in case the server's identity should be checked.
     * Should be {@code HTTPS}, {@code LDAPS} or {@code NONE} (default).
     * <p>
     * If set to {@code NONE}, it does not verify the hostname.
     * <p>
     */
    @WithDefault("NONE")
    String hostnameVerificationAlgorithm();

}
