package io.quarkus.grpc.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TlsClientConfig {

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
     * When used, {@code trust-certificate-jks} and {@code trust-certificate-p12} must not be used.
     */
    public PemTrustCertConfiguration trustCertificatePem;

    /**
     * Trust configuration in the JKS format.
     * <p>
     * When configured, {@code trust-certificate-pem} and {@code trust-certificate-p12} must not be used.
     */
    public JksConfiguration trustCertificateJks;

    /**
     * Trust configuration in the P12 format.
     * <p>
     * When configured, {@code trust-certificate-jks} and {@code trust-certificate-pem} must not be used.
     */
    public PfxConfiguration trustCertificateP12;

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * When configured, {@code key-certificate-jks} and {@code key-certificate-p12} must not be used.
     */
    public PemKeyCertConfiguration keyCertificatePem;

    /**
     * Key/cert configuration in the JKS format.
     * <p>
     * When configured, {@code #key-certificate-pem} and {@code #key-certificate-p12} must not be used.
     */
    public JksConfiguration keyCertificateJks;

    /**
     * Key/cert configuration in the P12 format.
     * <p>
     * When configured, {@code key-certificate-jks} and {@code #key-certificate-pem} must not be used.
     */
    public PfxConfiguration keyCertificateP12;

    /**
     * Whether hostname should be verified in the SSL/TLS handshake.
     */
    @ConfigItem(defaultValue = "true")
    public boolean verifyHostname;

    @ConfigGroup
    public static class PemTrustCertConfiguration {

        /**
         * Comma-separated list of the trust certificate files (Pem format).
         */
        @ConfigItem
        public Optional<List<String>> certs;

    }

    @ConfigGroup
    public static class JksConfiguration {

        /**
         * Path of the key file (JKS format).
         */
        @ConfigItem
        public Optional<String> path;

        /**
         * Password of the key file.
         */
        @ConfigItem
        public Optional<String> password;
    }

    @ConfigGroup
    public static class PfxConfiguration {

        /**
         * Path to the key file (PFX format).
         */
        @ConfigItem
        public Optional<String> path;

        /**
         * Password of the key.
         */
        @ConfigItem
        public Optional<String> password;
    }

    @ConfigGroup
    public static class PemKeyCertConfiguration {

        /**
         * Comma-separated list of the path to the key files (Pem format).
         */
        @ConfigItem
        public Optional<List<String>> keys;

        /**
         * Comma-separated list of the path to the certificate files (Pem format).
         */
        @ConfigItem
        public Optional<List<String>> certs;

    }

}
