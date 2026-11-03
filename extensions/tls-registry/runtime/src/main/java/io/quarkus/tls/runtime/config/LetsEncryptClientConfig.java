package io.quarkus.tls.runtime.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The runtime configuration of the Let's Encrypt client of the application.
 * <p>
 * It applies when {@code quarkus.tls.lets-encrypt.client.enabled} is {@code true} at build time.
 */
@ConfigMapping(prefix = "quarkus.tls.lets-encrypt.client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface LetsEncryptClientConfig {

    /**
     * The production directory of Let's Encrypt.
     */
    String PRODUCTION_DIRECTORY = "https://acme-v02.api.letsencrypt.org/directory";

    /**
     * The staging directory of Let's Encrypt.
     */
    String STAGING_DIRECTORY = "https://acme-staging-v02.api.letsencrypt.org/directory";

    /**
     * The domains of the certificate.
     * <p>
     * Each domain must resolve to the application, and port 80 of the domain must reach the HTTP server of the
     * application, because Let's Encrypt validates the domains with the HTTP-01 challenge.
     * Use the ASCII form of internationalized domain names.
     */
    Optional<List<String>> domains();

    /**
     * The email addresses of the Let's Encrypt account.
     */
    Optional<List<String>> emails();

    /**
     * Set to {@code true} to agree to the terms of service of the ACME server.
     * <p>
     * The client does not create an account without this agreement.
     */
    @WithDefault("false")
    boolean termsOfServiceAgreed();

    /**
     * The URL of the directory of the ACME server.
     * <p>
     * If not set, the client uses the Let's Encrypt production directory in production mode, and the Let's Encrypt
     * staging directory in dev and test modes.
     */
    Optional<String> directoryUrl();

    /**
     * The ACME profile of the certificate, for example {@code shortlived}.
     * <p>
     * If not set, the ACME server uses its default profile.
     */
    Optional<String> profile();

    /**
     * The directory that stores the account key, the certificate and its private key.
     * <p>
     * The client creates the directory if needed. Keep it on a persistent volume: without the stored certificate,
     * the application orders a new certificate at each start, and Let's Encrypt limits the number of duplicate
     * certificates.
     */
    @WithDefault(".letsencrypt")
    Path storageDirectory();

    /**
     * The interval between two renewal checks.
     * <p>
     * At each check, the client asks the ACME server when to renew the certificate (ACME Renewal Information,
     * RFC 9773). Without this information, the client renews the certificate when a third of its validity remains.
     */
    @WithDefault("12h")
    Duration renewalCheckInterval();

    /**
     * The name of the TLS configuration that the client uses to connect to the ACME server.
     * <p>
     * If not set, the client trusts the certificate authorities of the JVM. Set it to trust a private ACME server.
     */
    Optional<String> acmeServerTlsConfigurationName();
}
