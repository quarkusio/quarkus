package io.quarkus.redis.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;

@ConfigGroup
public class SslConfig {

    /**
     * Whether SSL/TLS is enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * Enable trusting all certificates. Disabled by default.
     */
    @ConfigItem(defaultValue = "false")
    public boolean trustAll;

    /**
     * Trust configuration in the PEM format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemTrustCertConfiguration trustCertificatePem;

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When enabled, {@code #trust-certificate-pem} and {@code #trust-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration trustCertificateJks;

    /**
     * Trust configuration in the PFX format.
     * <p>
     * When enabled, {@code #trust-certificate-jks} and {@code #trust-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration trustCertificatePfx;

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public PemKeyCertConfiguration keyCertificatePem;

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When enabled, {@code #key-certificate-pem} and {@code #key-certificate-pfx} must be disabled.
     */
    @ConfigItem
    public JksConfiguration keyCertificateJks;

    /**
     * Key/cert configuration in the PFX format.
     * <p>
     * When enabled, {@code key-certificate-jks} and {@code #key-certificate-pem} must be disabled.
     */
    @ConfigItem
    public PfxConfiguration keyCertificatePfx;

    /**
     * The hostname verification algorithm to use in case the server's identity should be checked.
     * Should be HTTPS, LDAPS or an empty string.
     */
    @ConfigItem
    public Optional<String> hostnameVerificationAlgorithm;

}
