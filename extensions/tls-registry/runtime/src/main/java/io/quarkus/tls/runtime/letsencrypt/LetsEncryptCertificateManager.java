package io.quarkus.tls.runtime.letsencrypt;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.KeyStoreProvider;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.LetsEncryptClientConfig;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.tls.runtime.letsencrypt.LetsEncryptStorage.StoredCertificate;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;

/**
 * The Let's Encrypt client of the application. It provides the key store of the TLS configuration, orders the
 * certificate, and renews it.
 * <p>
 * Until Let's Encrypt issues the first certificate, the key store holds a self-signed certificate for the
 * domains. After each new certificate, the manager reloads the TLS configuration and fires a
 * {@link CertificateUpdatedEvent}, so that the servers switch to the new certificate.
 */
public final class LetsEncryptCertificateManager implements KeyStoreProvider {

    private static final Logger LOGGER = Logger.getLogger(LetsEncryptCertificateManager.class);

    /**
     * The validity of the self-signed certificate that the application uses until the first order completes.
     */
    private static final Duration PLACEHOLDER_VALIDITY = Duration.ofDays(30);

    /**
     * How long the client waits for an authorization or an order to complete.
     */
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(5);

    /**
     * The delay before the first check, so that the HTTP server is listening when the ACME server validates
     * the challenge.
     */
    private static final Duration FIRST_CHECK_DELAY = Duration.ofSeconds(1);

    /**
     * The delay before a new attempt after the first failure. It doubles after each failure, up to the renewal
     * check interval, to stay under the rate limits of Let's Encrypt for failed validations.
     */
    private static final Duration FIRST_RETRY_DELAY = Duration.ofMinutes(10);

    private final Vertx vertx;
    private final String tlsConfigurationName;
    private final List<String> domains;
    private final List<String> contacts;
    private final String directoryUrl;
    private final Optional<String> profile;
    private final Duration checkInterval;
    private final Optional<String> acmeServerTlsConfigurationName;
    private final LetsEncryptStorage storage;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile Material material;
    private volatile boolean domainsChanged;
    private volatile boolean closed;
    private volatile long timerId = -1;
    private TlsConfigurationRegistry registry;
    private HttpClient client;
    private int failures;

    /**
     * The certificate chain and the private key that the key store holds.
     *
     * @param certificate the certificate and its key
     * @param selfSigned {@code true} for the temporary self-signed certificate
     */
    private record Material(StoredCertificate certificate, boolean selfSigned) {
    }

    private LetsEncryptCertificateManager(Vertx vertx, LetsEncryptClientConfig config, String tlsConfigurationName,
            List<String> domains, String directoryUrl) {
        this.vertx = vertx;
        this.tlsConfigurationName = tlsConfigurationName;
        this.domains = domains;
        this.contacts = config.emails().orElse(List.of()).stream().map(email -> "mailto:" + email).toList();
        this.directoryUrl = directoryUrl;
        this.profile = config.profile();
        this.checkInterval = config.renewalCheckInterval();
        this.acmeServerTlsConfigurationName = config.acmeServerTlsConfigurationName();
        this.storage = new LetsEncryptStorage(config.storageDirectory());
    }

    /**
     * Validate the configuration and load the stored certificate, or create the self-signed certificate.
     *
     * @param vertx the Vert.x instance
     * @param config the configuration of the client
     * @param tlsConfigurationName the name of the TLS configuration that uses the certificate
     * @param launchMode the launch mode, which selects the default ACME directory
     * @return the manager
     */
    public static LetsEncryptCertificateManager create(Vertx vertx, LetsEncryptClientConfig config,
            String tlsConfigurationName, LaunchMode launchMode) {
        List<String> domains = config.domains().orElse(List.of());
        if (domains.isEmpty()) {
            throw new ConfigurationException(
                    "The Let's Encrypt client needs at least one domain in quarkus.tls.lets-encrypt.client.domains",
                    Set.of("quarkus.tls.lets-encrypt.client.domains"));
        }
        if (!config.termsOfServiceAgreed()) {
            throw new ConfigurationException(
                    "The Let's Encrypt client needs the agreement to the terms of service of the ACME server: set "
                            + "quarkus.tls.lets-encrypt.client.terms-of-service-agreed to true",
                    Set.of("quarkus.tls.lets-encrypt.client.terms-of-service-agreed"));
        }
        for (String domain : domains) {
            if (!StandardCharsets.US_ASCII.newEncoder().canEncode(domain)) {
                throw new ConfigurationException("The Let's Encrypt domain " + domain
                        + " is not ASCII: use the ASCII form of internationalized domain names",
                        Set.of("quarkus.tls.lets-encrypt.client.domains"));
            }
        }
        String directoryUrl = config.directoryUrl().orElse(launchMode == LaunchMode.NORMAL
                ? LetsEncryptClientConfig.PRODUCTION_DIRECTORY
                : LetsEncryptClientConfig.STAGING_DIRECTORY);
        LetsEncryptCertificateManager manager = new LetsEncryptCertificateManager(vertx, config, tlsConfigurationName,
                List.copyOf(domains), directoryUrl);
        manager.loadMaterial();
        return manager;
    }

    @Override
    public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
        StoredCertificate certificate = material.certificate();
        try {
            PemKeyCertOptions options = new PemKeyCertOptions()
                    .setCertValue(Buffer.buffer(Certificates.certificatesPem(certificate.chain())))
                    .setKeyValue(Buffer.buffer(Certificates.privateKeyPem(certificate.key())));
            return new KeyStoreAndKeyCertOptions(options.loadKeyStore(vertx), options);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create the key store of the Let's Encrypt certificate", e);
        }
    }

    /**
     * Start the renewal checks.
     *
     * @param registry the TLS registry, to reload the TLS configuration and to find the ACME server trust
     */
    public void start(TlsConfigurationRegistry registry) {
        this.registry = registry;
        HttpClientOptions options = new HttpClientOptions();
        if (acmeServerTlsConfigurationName.isPresent()) {
            TlsConfiguration acmeTls = registry.get(acmeServerTlsConfigurationName.get())
                    .orElseThrow(() -> new ConfigurationException("The TLS configuration "
                            + acmeServerTlsConfigurationName.get() + " of the ACME server does not exist",
                            Set.of("quarkus.tls.lets-encrypt.client.acme-server-tls-configuration-name")));
            if (acmeTls.getTrustStoreOptions() != null) {
                options.setTrustOptions(acmeTls.getTrustStoreOptions());
            }
        }
        this.client = vertx.createHttpClient(options);
        LOGGER.infof("Let's Encrypt client started for %s with the ACME directory %s", domains, directoryUrl);
        schedule(FIRST_CHECK_DELAY);
    }

    /**
     * Stop the renewal checks.
     */
    public void close() {
        closed = true;
        if (timerId >= 0) {
            vertx.cancelTimer(timerId);
        }
        if (client != null) {
            client.close();
        }
    }

    private void loadMaterial() {
        try {
            Optional<StoredCertificate> stored = storage.certificate();
            if (stored.isPresent()) {
                material = new Material(stored.get(), false);
                domainsChanged = !coversDomains(stored.get().certificate());
                if (domainsChanged) {
                    LOGGER.infof("The stored Let's Encrypt certificate does not cover %s, the client orders a new one",
                            domains);
                }
                return;
            }
            KeyPair keyPair = Certificates.generateKeyPair();
            X509Certificate selfSigned = Certificates.selfSigned(keyPair, domains, PLACEHOLDER_VALIDITY);
            material = new Material(new StoredCertificate(List.of(selfSigned), keyPair.getPrivate()), true);
            LOGGER.infof("No Let's Encrypt certificate is stored yet, the application uses a self-signed certificate "
                    + "until the first order completes");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load the Let's Encrypt certificate from " + storage, e);
        }
    }

    private boolean coversDomains(X509Certificate certificate) throws CertificateParsingException {
        Collection<List<?>> names = certificate.getSubjectAlternativeNames();
        List<String> dnsNames = new ArrayList<>();
        if (names != null) {
            for (List<?> name : names) {
                if (Integer.valueOf(2).equals(name.get(0))) {
                    dnsNames.add(String.valueOf(name.get(1)));
                }
            }
        }
        return dnsNames.containsAll(domains);
    }

    private void schedule(Duration delay) {
        if (closed) {
            return;
        }
        timerId = vertx.setTimer(Math.max(1, delay.toMillis()), id -> check());
    }

    private void check() {
        if (closed || !running.compareAndSet(false, true)) {
            return;
        }
        AcmeClient acme;
        try {
            acme = new AcmeClient(vertx, client, directoryUrl, new Jws(storage.accountKey()), POLL_TIMEOUT);
        } catch (Exception e) {
            failed(e);
            return;
        }
        renewalTime(acme)
                .compose(renewal -> {
                    Instant now = Instant.now();
                    if (renewal.isAfter(now)) {
                        return Future.succeededFuture(renewal);
                    }
                    return order(acme).map(now);
                })
                .onSuccess(renewal -> {
                    failures = 0;
                    running.set(false);
                    Duration untilRenewal = Duration.between(Instant.now(), renewal);
                    if (untilRenewal.isNegative() || untilRenewal.isZero()) {
                        // a new certificate was issued: check its renewal window at the next check
                        schedule(jitter(checkInterval));
                    } else {
                        schedule(untilRenewal.compareTo(checkInterval) < 0 ? untilRenewal : jitter(checkInterval));
                    }
                })
                .onFailure(this::failed);
    }

    private void failed(Throwable failure) {
        running.set(false);
        failures++;
        Duration delay = FIRST_RETRY_DELAY.multipliedBy(1L << Math.min(failures - 1, 16));
        if (delay.compareTo(checkInterval) > 0) {
            delay = checkInterval;
        }
        Material current = material;
        String expiry = current.selfSigned() ? "the application still uses a self-signed certificate"
                : "the current certificate expires on " + current.certificate().certificate().getNotAfter().toInstant();
        LOGGER.errorf(failure, "The Let's Encrypt order for %s failed (%s); next attempt in %s", domains, expiry, delay);
        schedule(delay);
    }

    /**
     * Compute when to renew the current certificate: now for the self-signed certificate or a changed domain
     * list, a random time in the window of ACME Renewal Information when the server provides it, or the time
     * when a third of the validity remains.
     */
    private Future<Instant> renewalTime(AcmeClient acme) {
        Material current = material;
        if (current.selfSigned() || domainsChanged) {
            return Future.succeededFuture(Instant.now());
        }
        X509Certificate certificate = current.certificate().certificate();
        Instant fallback = fallbackRenewalTime(certificate);
        String certificateId = Certificates.renewalCertificateId(certificate);
        if (certificateId == null) {
            return Future.succeededFuture(fallback);
        }
        return acme.renewalWindow(certificateId)
                .map(window -> window.map(w -> randomInstant(w.start(), w.end())).orElse(fallback))
                .recover(e -> {
                    LOGGER.warnf("Unable to get the ACME renewal information of the certificate: %s", e.getMessage());
                    return Future.succeededFuture(fallback);
                });
    }

    private Future<Void> order(AcmeClient acme) {
        Material current = material;
        Optional<String> replaces = current.selfSigned() || domainsChanged
                ? Optional.empty()
                : Optional.ofNullable(Certificates.renewalCertificateId(current.certificate().certificate()));
        KeyPair keyPair;
        try {
            keyPair = Certificates.generateKeyPair();
        } catch (GeneralSecurityException e) {
            return Future.failedFuture(e);
        }
        LOGGER.infof("Ordering a Let's Encrypt certificate for %s", domains);
        return acme.account(contacts)
                .compose(account -> acme.newOrder(domains, profile, replaces))
                .compose(created -> {
                    String orderUrl = created.location();
                    JsonObject order = created.json();
                    return authorizeAll(acme, order.getJsonArray("authorizations", new JsonArray()), 0)
                            .compose(v -> {
                                byte[] csr;
                                try {
                                    csr = Certificates.certificateSigningRequest(keyPair, domains);
                                } catch (GeneralSecurityException e) {
                                    return Future.failedFuture(e);
                                }
                                return acme.finalizeOrder(orderUrl, order, csr);
                            });
                })
                .compose(acme::downloadCertificate)
                .compose(pem -> vertx.executeBlocking(() -> {
                    List<X509Certificate> chain = Certificates.readCertificates(pem);
                    if (!chain.get(0).getPublicKey().equals(keyPair.getPublic())) {
                        throw new GeneralSecurityException("The issued certificate does not match the requested key");
                    }
                    StoredCertificate issued = new StoredCertificate(chain, keyPair.getPrivate());
                    storage.store(issued);
                    material = new Material(issued, false);
                    domainsChanged = false;
                    LOGGER.infof("Let's Encrypt issued a certificate for %s, valid until %s", domains,
                            chain.get(0).getNotAfter().toInstant());
                    reload();
                    return null;
                }, false));
    }

    private Future<Void> authorizeAll(AcmeClient acme, JsonArray authorizations, int index) {
        if (index >= authorizations.size()) {
            return Future.succeededFuture();
        }
        AcmeClient.ChallengePublisher publisher = new AcmeClient.ChallengePublisher() {
            @Override
            public void publish(String token, String keyAuthorization) {
                LetsEncryptChallenges.publish(token, keyAuthorization);
            }

            @Override
            public void remove(String token) {
                LetsEncryptChallenges.remove(token);
            }
        };
        return acme.authorize(authorizations.getString(index), publisher)
                .compose(v -> authorizeAll(acme, authorizations, index + 1));
    }

    private void reload() {
        Optional<TlsConfiguration> configuration = TlsConfig.DEFAULT_NAME.equals(tlsConfigurationName)
                ? registry.getDefault()
                : registry.get(tlsConfigurationName);
        if (configuration.isEmpty()) {
            LOGGER.warnf("The TLS configuration %s does not exist, the new certificate is stored but not used",
                    tlsConfigurationName);
            return;
        }
        if (configuration.get().reload()) {
            Arc.container().beanManager().getEvent().select(CertificateUpdatedEvent.class)
                    .fire(new CertificateUpdatedEvent(tlsConfigurationName, configuration.get()));
        } else {
            LOGGER.errorf("Unable to reload the TLS configuration %s with the new certificate", tlsConfigurationName);
        }
    }

    static Instant fallbackRenewalTime(X509Certificate certificate) {
        Instant notBefore = certificate.getNotBefore().toInstant();
        Instant notAfter = certificate.getNotAfter().toInstant();
        Duration lifetime = Duration.between(notBefore, notAfter);
        // Let's Encrypt advises to renew short-lived certificates (under 10 days) at half of their lifetime
        long remainingFraction = lifetime.compareTo(Duration.ofDays(10)) < 0 ? 2 : 3;
        return notAfter.minus(lifetime.dividedBy(remainingFraction));
    }

    private static Instant randomInstant(Instant start, Instant end) {
        long span = Duration.between(start, end).toMillis();
        return span <= 0 ? start : start.plusMillis(ThreadLocalRandom.current().nextLong(span));
    }

    private static Duration jitter(Duration interval) {
        // spread the checks of many applications over time, as Let's Encrypt advises
        long millis = interval.toMillis();
        return Duration.ofMillis(millis - millis / 10 + ThreadLocalRandom.current().nextLong(Math.max(1, millis / 5)));
    }
}
