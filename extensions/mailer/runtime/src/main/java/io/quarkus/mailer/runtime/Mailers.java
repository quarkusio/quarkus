package io.quarkus.mailer.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Event;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.mailer.SentMail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.mail.CanonicalizationAlgorithm;
import io.vertx.ext.mail.DKIMSignOptions;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;

/**
 * This class is a sort of producer for mailer instances.
 * <p>
 * It isn't a CDI producer in the literal sense, but it creates a synthetic bean
 * from {@code MailerProcessor}.
 */
@Singleton
public class Mailers {

    private static final Logger LOGGER = Logger.getLogger(Mailers.class);

    public static final String DEFAULT_MAILER_NAME = "<default>";

    private final Map<String, MailClient> clients;
    private final Map<String, io.vertx.mutiny.ext.mail.MailClient> mutinyClients;
    private final Map<String, MockMailboxImpl> mockMailboxes;
    private final Map<String, MutinyMailerImpl> mutinyMailers;

    public Mailers(Vertx vertx, io.vertx.mutiny.core.Vertx mutinyVertx, MailersRuntimeConfig mailersRuntimeConfig,
            LaunchMode launchMode, MailerSupport mailerSupport, TlsConfigurationRegistry tlsRegistry,
            Event<SentMail> sentMailEvent) {
        Map<String, MailClient> localClients = new HashMap<>();
        Map<String, io.vertx.mutiny.ext.mail.MailClient> localMutinyClients = new HashMap<>();
        Map<String, MockMailboxImpl> localMockMailboxes = new HashMap<>();
        Map<String, MutinyMailerImpl> localMutinyMailers = new HashMap<>();

        if (mailerSupport.hasDefaultMailer) {
            MailerRuntimeConfig defaultMailerConfig = mailersRuntimeConfig.mailers().get(DEFAULT_MAILER_NAME);

            MailClient mailClient = createMailClient(vertx, DEFAULT_MAILER_NAME, defaultMailerConfig,
                    tlsRegistry);
            io.vertx.mutiny.ext.mail.MailClient mutinyMailClient = io.vertx.mutiny.ext.mail.MailClient.newInstance(mailClient);
            MockMailboxImpl mockMailbox = new MockMailboxImpl();
            localClients.put(DEFAULT_MAILER_NAME, mailClient);
            localMutinyClients.put(DEFAULT_MAILER_NAME, mutinyMailClient);
            localMockMailboxes.put(DEFAULT_MAILER_NAME, mockMailbox);
            localMutinyMailers.put(DEFAULT_MAILER_NAME,
                    new MutinyMailerImpl(mutinyVertx, mutinyMailClient, mockMailbox,
                            defaultMailerConfig.from().orElse(null),
                            defaultMailerConfig.bounceAddress().orElse(null),
                            defaultMailerConfig.mock().orElse(launchMode.isDevOrTest()),
                            defaultMailerConfig.approvedRecipients().orElse(List.of()).stream()
                                    .filter(Objects::nonNull).collect(Collectors.toList()),
                            defaultMailerConfig.logRejectedRecipients(),
                            defaultMailerConfig.logInvalidRecipients(), sentMailEvent));
        }

        for (String name : mailerSupport.namedMailers) {
            MailerRuntimeConfig namedMailerRuntimeConfig = mailersRuntimeConfig.mailers().get(name);

            MailClient namedMailClient = createMailClient(vertx, name, namedMailerRuntimeConfig,
                    tlsRegistry);
            io.vertx.mutiny.ext.mail.MailClient namedMutinyMailClient = io.vertx.mutiny.ext.mail.MailClient
                    .newInstance(namedMailClient);
            MockMailboxImpl namedMockMailbox = new MockMailboxImpl();
            localClients.put(name, namedMailClient);
            localMutinyClients.put(name, namedMutinyMailClient);
            localMockMailboxes.put(name, namedMockMailbox);
            localMutinyMailers.put(name,
                    new MutinyMailerImpl(mutinyVertx, namedMutinyMailClient, namedMockMailbox,
                            namedMailerRuntimeConfig.from().orElse(null),
                            namedMailerRuntimeConfig.bounceAddress().orElse(null),
                            namedMailerRuntimeConfig.mock().orElse(false),
                            namedMailerRuntimeConfig.approvedRecipients().orElse(List.of()).stream()
                                    .filter(p -> p != null).collect(Collectors.toList()),
                            namedMailerRuntimeConfig.logRejectedRecipients(),
                            namedMailerRuntimeConfig.logInvalidRecipients(),
                            sentMailEvent));
        }

        this.clients = Collections.unmodifiableMap(localClients);
        this.mutinyClients = Collections.unmodifiableMap(localMutinyClients);
        this.mockMailboxes = Collections.unmodifiableMap(localMockMailboxes);
        this.mutinyMailers = Collections.unmodifiableMap(localMutinyMailers);
    }

    public MailClient mailClientFromName(String name) {
        return clients.get(name);
    }

    public io.vertx.mutiny.ext.mail.MailClient reactiveMailClientFromName(String name) {
        return mutinyClients.get(name);
    }

    public Mailer mailerFromName(String name) {
        return new BlockingMailerImpl(reactiveMailerFromName(name));
    }

    public ReactiveMailer reactiveMailerFromName(String name) {
        return mutinyMailers.get(name);
    }

    public MockMailbox mockMailboxFromName(String name) {
        return mockMailboxes.get(name);
    }

    @PreDestroy
    public void stop() {
        for (MailClient client : clients.values()) {
            client.close();
        }
    }

    private MailClient createMailClient(Vertx vertx, String name, MailerRuntimeConfig config,
            TlsConfigurationRegistry tlsRegistry) {
        io.vertx.ext.mail.MailConfig cfg = toVertxMailConfig(name, config, tlsRegistry);
        // Do not create a shared instance, as we want separated connection pool for each SMTP servers.
        return MailClient.create(vertx, cfg);
    }

    private io.vertx.ext.mail.DKIMSignOptions toVertxDkimSignOptions(DkimSignOptionsConfig optionsConfig) {
        DKIMSignOptions vertxDkimOptions = new io.vertx.ext.mail.DKIMSignOptions();

        String sdid = optionsConfig.sdid()
                .orElseThrow(() -> {
                    throw new ConfigurationException("Must provide the Signing Domain Identifier (sdid).");
                });
        vertxDkimOptions.setSdid(sdid);

        String selector = optionsConfig.selector()
                .orElseThrow(() -> {
                    throw new ConfigurationException("Must provide the selector.");
                });
        vertxDkimOptions.setSelector(selector);

        if (optionsConfig.auid().isPresent()) {
            vertxDkimOptions.setAuid(optionsConfig.auid().get());
        }

        if (optionsConfig.bodyLimit().isPresent()) {
            int bodyLimit = optionsConfig.bodyLimit().getAsInt();
            vertxDkimOptions.setBodyLimit(bodyLimit);
        }

        if (optionsConfig.expireTime().isPresent()) {
            long expireTime = optionsConfig.expireTime().getAsLong();
            vertxDkimOptions.setExpireTime(expireTime);
        }

        if (optionsConfig.bodyCanonAlgo().isPresent()) {
            vertxDkimOptions
                    .setBodyCanonAlgo(CanonicalizationAlgorithm.valueOf(optionsConfig.bodyCanonAlgo().get().toString()));
        }

        if (optionsConfig.headerCanonAlgo().isPresent()) {
            vertxDkimOptions
                    .setHeaderCanonAlgo(CanonicalizationAlgorithm.valueOf(optionsConfig.headerCanonAlgo().get().toString()));
        }

        if (optionsConfig.privateKey().isPresent()) {
            vertxDkimOptions.setPrivateKey(optionsConfig.privateKey().get());
        } else if (optionsConfig.privateKeyPath().isPresent()) {
            vertxDkimOptions.setPrivateKeyPath(optionsConfig.privateKeyPath().get());
        }

        if (optionsConfig.signatureTimestamp().isPresent()) {
            vertxDkimOptions.setSignatureTimestamp(optionsConfig.signatureTimestamp().get());
        }

        if (optionsConfig.signedHeaders().isPresent()) {
            List<String> headers = optionsConfig.signedHeaders().get();

            if (headers.stream().noneMatch(header -> header.equalsIgnoreCase("from"))) {
                throw new ConfigurationException(
                        "The \"From\" header must always be included to the list of headers to sign.");
            }

            vertxDkimOptions.setSignedHeaders(headers);
        }

        return vertxDkimOptions;
    }

    private io.vertx.ext.mail.MailConfig toVertxMailConfig(String name, MailerRuntimeConfig config,
            TlsConfigurationRegistry tlsRegistry) {
        io.vertx.ext.mail.MailConfig cfg = new io.vertx.ext.mail.MailConfig();
        if (config.authMethods().isPresent()) {
            cfg.setAuthMethods(config.authMethods().get());
        }
        cfg.setDisableEsmtp(config.disableEsmtp());
        cfg.setHostname(config.host());
        cfg.setKeepAlive(config.keepAlive());
        cfg.setLogin(LoginOption.valueOf(config.login().toUpperCase()));
        cfg.setMaxPoolSize(config.maxPoolSize());

        if (config.ownHostName().isPresent()) {
            cfg.setOwnHostname(config.ownHostName().get());
        }

        if (config.username().isPresent()) {
            cfg.setUsername(config.username().get());
        }
        if (config.password().isPresent()) {
            cfg.setPassword(config.password().get());
        }

        if (config.port().isPresent()) {
            cfg.setPort(config.port().getAsInt());
        }

        if (config.dkim() != null && config.dkim().enabled()) {
            cfg.setEnableDKIM(true);
            cfg.addDKIMSignOption(toVertxDkimSignOptions(config.dkim()));
        }

        cfg.setStarttls(StartTLSOptions.valueOf(config.startTLS().toUpperCase()));
        cfg.setMultiPartOnly(config.multiPartOnly());

        cfg.setAllowRcptErrors(config.allowRcptErrors());
        cfg.setPipelining(config.pipelining());
        cfg.setPoolCleanerPeriod((int) config.poolCleanerPeriod().toMillis());
        cfg.setPoolCleanerPeriodUnit(TimeUnit.MILLISECONDS);
        cfg.setKeepAliveTimeout((int) config.keepAliveTimeout().toMillis());
        cfg.setKeepAliveTimeoutUnit(TimeUnit.MILLISECONDS);

        configureTLS(name, config, tlsRegistry, cfg);

        // Sets the metrics name so micrometer metrics will collect metrics for the client.
        // Because the mail client is _unnamed_, we only pass a prefix.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        cfg.setMetricsName("mail");

        return cfg;
    }

    private void configureTLS(String name, MailerRuntimeConfig config, TlsConfigurationRegistry tlsRegistry, MailConfig cfg) {
        TlsConfiguration configuration = null;
        boolean defaultTrustAll = false;
        if (config.tlsConfigurationName().isPresent()) {
            Optional<TlsConfiguration> maybeConfiguration = tlsRegistry.get(config.tlsConfigurationName().get());
            if (!maybeConfiguration.isPresent()) {
                throw new IllegalStateException("Unable to find the TLS configuration "
                        + config.tlsConfigurationName().get() + " for the mailer " + name + ".");
            }
            configuration = maybeConfiguration.get();
        } else if (tlsRegistry.getDefault().isPresent() && tlsRegistry.getDefault().get().isTrustAll()) {
            defaultTrustAll = tlsRegistry.getDefault().get().isTrustAll();
            if (defaultTrustAll) {
                LOGGER.warn("The default TLS configuration is set to trust all certificates. This is a security risk."
                        + "Please use a named TLS configuration for the mailer " + name + " to avoid this warning.");
            }
        }

        if (configuration != null) {
            // SMTP is a bit convoluted here.
            // You can start a non-TLS connection and then upgrade to TLS (using the STARTTLS command).
            cfg.setSsl(config.tls().orElse(true));

            if (configuration.getTrustStoreOptions() != null) {
                cfg.setTrustOptions(configuration.getTrustStoreOptions());
            }
            if (configuration.getKeyStoreOptions() != null) {
                cfg.setKeyCertOptions(configuration.getKeyStoreOptions());
            }

            if (configuration.isTrustAll()) {
                cfg.setTrustAll(true);
            }
            if (configuration.getHostnameVerificationAlgorithm().isPresent()) {
                cfg.setHostnameVerificationAlgorithm(configuration.getHostnameVerificationAlgorithm().get());
            }

            SSLOptions sslOptions = configuration.getSSLOptions();
            if (sslOptions != null) {
                cfg.setSslHandshakeTimeout(sslOptions.getSslHandshakeTimeout());
                cfg.setSslHandshakeTimeoutUnit(sslOptions.getSslHandshakeTimeoutUnit());
                for (String suite : sslOptions.getEnabledCipherSuites()) {
                    cfg.addEnabledCipherSuite(suite);
                }
                for (Buffer buffer : sslOptions.getCrlValues()) {
                    cfg.addCrlValue(buffer);
                }
                cfg.setEnabledSecureTransportProtocols(sslOptions.getEnabledSecureTransportProtocols());

            }

        } else {
            boolean trustAll = config.trustAll().isPresent() ? config.trustAll().get() : defaultTrustAll;
            cfg.setSsl(config.ssl() || config.tls().orElse(trustAll));
            cfg.setTrustAll(trustAll);
            applyTruststore(name, config, cfg);
        }
    }

    private void applyTruststore(String name, MailerRuntimeConfig config, io.vertx.ext.mail.MailConfig cfg) {
        // Handle deprecated config
        if (config.keyStore().isPresent()) {
            LOGGER.warn("`quarkus.mailer.key-store` is deprecated, use `quarkus.mailer.trust-store.path` instead");
            JksOptions options = new JksOptions();
            options.setPath(config.keyStore().get());
            if (config.keyStorePassword().isPresent()) {
                LOGGER.warn(
                        "`quarkus.mailer.key-store-password` is deprecated, use `quarkus.mailer.trust-store.password` instead");
                options.setPassword(config.keyStorePassword().get());
            }
            cfg.setTrustOptions(options);
            return;
        }

        TrustStoreConfig truststore = config.truststore();
        if (truststore.isConfigured()) {
            if (cfg.isTrustAll()) { // Use the value configured before.
                LOGGER.warn(
                        "SMTP is configured with a trust store and also with trust-all, disable trust-all to enforce the trust store usage");
            }
            cfg.setTrustOptions(getTrustOptions(name, truststore.password(), truststore.paths(), truststore.type()));
        }
    }

    private TrustOptions getTrustOptions(String name, Optional<String> pwd, Optional<List<String>> paths,
            Optional<String> type) {
        if (!paths.isPresent()) {
            throw new ConfigurationException("Expected SMTP trust store `paths` to have at least one value");
        }
        List<String> actualPaths = paths.get();
        if (actualPaths.isEmpty()) {
            throw new ConfigurationException("Expected SMTP trust store `paths` to have at least one value");
        }

        if (type.isPresent()) {
            String actualType = type.get();
            if (actualType.equalsIgnoreCase("JKS")) {
                return configureJksTrustOptions(actualPaths, pwd);
            } else if (actualType.equalsIgnoreCase("PKCS")) {
                return configurePkcsTrustOptions(actualPaths, pwd);
            } else if (actualType.equalsIgnoreCase("PEM")) {
                return configurePemTrustOptions(actualPaths, pwd);
            } else {
                throw new ConfigurationException("Unsupported value for the SMTP trust store type. The value (" + actualType
                        + ") must be JKS, PKCS or PEM");
            }
        }

        String firstPath = actualPaths.get(0).toLowerCase();
        if (firstPath.endsWith(".jks")) {
            return configureJksTrustOptions(actualPaths, pwd);
        } else if (firstPath.endsWith(".p12") || firstPath.endsWith(".pfx")) {
            return configurePkcsTrustOptions(actualPaths, pwd);
        } else if (firstPath.endsWith(".pem") || firstPath.endsWith(".crt")) {
            return configurePemTrustOptions(actualPaths, pwd);
        }

        if (DEFAULT_MAILER_NAME.equals(name)) {
            throw new ConfigurationException(
                    "Unable to deduce the SMTP trust store type from the file name. Configure `quarkus.mailer.truststore.type` explicitly");
        } else {
            throw new ConfigurationException(
                    "Unable to deduce the SMTP trust store type from the file name. Configure `quarkus.mailer." + name
                            + ".truststore.type` explicitly");
        }

    }

    private TrustOptions configureJksTrustOptions(List<String> paths, Optional<String> pwd) {
        JksOptions options = new JksOptions();
        options.setPassword(pwd.orElse(null));
        if (paths.size() > 1) {
            throw new ConfigurationException(
                    "Invalid SMTP trust store configuration, JKS only supports a single file, found " + paths.size());
        }
        options.setPath(paths.get(0).trim());
        return options;
    }

    private TrustOptions configurePkcsTrustOptions(List<String> paths, Optional<String> pwd) {
        PfxOptions options = new PfxOptions();
        options.setPassword(pwd.orElse(null));
        if (paths.size() > 1) {
            throw new ConfigurationException(
                    "Invalid SMTP trust store configuration, PFX only supports a single file, found " + paths.size());
        }
        options.setPath(paths.get(0).trim());
        return options;
    }

    private TrustOptions configurePemTrustOptions(List<String> paths, Optional<String> pwd) {
        PemTrustOptions options = new PemTrustOptions();
        if (pwd.isPresent()) {
            throw new ConfigurationException("Invalid SMTP trust store configuration, PEM trust store to not support password");
        }
        for (String path : paths) {
            options.addCertPath(path.trim());
        }
        return options;
    }

}
