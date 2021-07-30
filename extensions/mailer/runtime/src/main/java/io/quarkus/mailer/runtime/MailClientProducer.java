package io.quarkus.mailer.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.StartTLSOptions;

/**
 * Beans producing the Vert.x Mail clients.
 */
@ApplicationScoped
public class MailClientProducer {

    private static final Logger LOGGER = Logger.getLogger(MailClientProducer.class);

    private final io.vertx.mutiny.ext.mail.MailClient mutinyClient;
    private final MailClient client;

    public MailClientProducer(Vertx vertx, MailConfig config, TlsConfig globalTlsConfig) {
        this.client = mailClient(vertx, config, globalTlsConfig);
        this.mutinyClient = io.vertx.mutiny.ext.mail.MailClient.newInstance(this.client);
    }

    @Singleton
    @Produces
    public MailClient mailClient() {
        return client;
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.ext.mail.MailClient mutinyClient() {
        return mutinyClient;
    }

    @PreDestroy
    public void stop() {
        client.close();
    }

    private MailClient mailClient(Vertx vertx, MailConfig config, TlsConfig tlsConfig) {
        io.vertx.ext.mail.MailConfig cfg = toVertxMailConfig(config, tlsConfig);
        return MailClient.createShared(vertx, cfg);
    }

    private io.vertx.ext.mail.MailConfig toVertxMailConfig(MailConfig config, TlsConfig tlsConfig) {
        io.vertx.ext.mail.MailConfig cfg = new io.vertx.ext.mail.MailConfig();
        if (config.authMethods.isPresent()) {
            cfg.setAuthMethods(config.authMethods.get());
        }
        cfg.setDisableEsmtp(config.disableEsmtp);
        cfg.setHostname(config.host);
        cfg.setKeepAlive(config.keepAlive);
        cfg.setLogin(LoginOption.valueOf(config.login.toUpperCase()));
        cfg.setMaxPoolSize(config.maxPoolSize);

        if (config.ownHostName.isPresent()) {
            cfg.setOwnHostname(config.ownHostName.get());
        }

        if (config.username.isPresent()) {
            cfg.setUsername(config.username.get());
        }
        if (config.password.isPresent()) {
            cfg.setPassword(config.password.get());
        }

        if (config.port.isPresent()) {
            cfg.setPort(config.port.getAsInt());
        }

        cfg.setSsl(config.ssl);
        cfg.setStarttls(StartTLSOptions.valueOf(config.startTLS.toUpperCase()));
        cfg.setMultiPartOnly(config.multiPartOnly);

        cfg.setAllowRcptErrors(config.allowRcptErrors);
        cfg.setPipelining(config.pipelining);
        cfg.setPoolCleanerPeriod((int) config.poolCleanerPeriod.toMillis());
        cfg.setPoolCleanerPeriodUnit(TimeUnit.MILLISECONDS);
        cfg.setKeepAliveTimeout((int) config.keepAliveTimeout.toMillis());
        cfg.setKeepAliveTimeoutUnit(TimeUnit.MILLISECONDS);

        boolean trustAll = config.trustAll.isPresent() ? config.trustAll.get() : tlsConfig.trustAll;
        cfg.setTrustAll(trustAll);
        applyTruststore(config, cfg);

        return cfg;
    }

    private void applyTruststore(MailConfig config, io.vertx.ext.mail.MailConfig cfg) {
        // Handle deprecated config
        if (config.keyStore.isPresent()) {
            LOGGER.warn("`quarkus.mailer.key-store` is deprecated, use `quarkus.mailer.trust-store.path` instead");
            JksOptions options = new JksOptions();
            options.setPath(config.keyStore.get());
            if (config.keyStorePassword.isPresent()) {
                LOGGER.warn(
                        "`quarkus.mailer.key-store-password` is deprecated, use `quarkus.mailer.trust-store.password` instead");
                options.setPassword(config.keyStorePassword.get());
            }
            cfg.setTrustOptions(options);
            return;
        }

        TrustStoreConfig truststore = config.truststore;
        if (truststore.isConfigured()) {
            if (cfg.isTrustAll()) { // USe the value configured before.
                LOGGER.warn(
                        "SMTP is configured with a trust store and also with trust-all, disable trust-all to enforce the trust store usage");
            }
            cfg.setTrustOptions(getTrustOptions(truststore.password, truststore.paths, truststore.type));
        }
    }

    private TrustOptions getTrustOptions(Optional<String> pwd, Optional<List<String>> paths, Optional<String> type) {
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
                return configurePcksTrustOptions(actualPaths, pwd);
            } else if (actualType.equalsIgnoreCase("PEM")) {
                return configurePemTrustOptions(actualPaths, pwd);
            } else {
                throw new ConfigurationException("Unsupported value for the SMTP trust store type. The value (" + actualType
                        + ") must be JKS, PCKS or PEM");
            }
        }

        String firstPath = actualPaths.get(0).toLowerCase();
        if (firstPath.endsWith(".jks")) {
            return configureJksTrustOptions(actualPaths, pwd);
        } else if (firstPath.endsWith(".p12") || firstPath.endsWith(".pfx")) {
            return configurePcksTrustOptions(actualPaths, pwd);
        } else if (firstPath.endsWith(".pem")) {
            return configurePemTrustOptions(actualPaths, pwd);
        }

        throw new ConfigurationException(
                "Unable to deduce the SMTP trust store type from the file name. Configure `quarkus.mailer.trust-store.type` explicitly");

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

    private TrustOptions configurePcksTrustOptions(List<String> paths, Optional<String> pwd) {
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
