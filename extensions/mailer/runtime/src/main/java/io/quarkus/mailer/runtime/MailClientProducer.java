package io.quarkus.mailer.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.vertx.core.Vertx;
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

    public MailClientProducer(Vertx vertx, MailConfig config, TlsConfig tlsConfig) {
        this.client = mailClient(vertx, config, tlsConfig);
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
        if (config.keyStore.isPresent()) {
            cfg.setKeyStore(config.keyStore.get());
        }
        if (config.keyStorePassword.isPresent()) {
            cfg.setKeyStorePassword(config.keyStorePassword.get());
        }
        if (config.login.isPresent()) {
            cfg.setLogin(LoginOption.valueOf(config.login.get().toUpperCase()));
        }
        if (config.maxPoolSize.isPresent()) {
            cfg.setMaxPoolSize(config.maxPoolSize.getAsInt());
        }
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
        if (config.startTLS.isPresent()) {
            cfg.setStarttls(StartTLSOptions.valueOf(config.startTLS.get().toUpperCase()));
        }
        cfg.setMultiPartOnly(config.multiPartOnly);
        boolean trustAll = config.trustAll.isPresent() ? config.trustAll.get() : tlsConfig.trustAll;
        cfg.setTrustAll(trustAll);
        return cfg;
    }

}
