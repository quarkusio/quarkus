package io.quarkus.mailer.runtime;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

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

    public MailClientProducer(Vertx vertx, MailConfig config) {
        this.client = mailClient(vertx, config);
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

    @Singleton
    @Produces
    @Deprecated
    public io.vertx.axle.ext.mail.MailClient axleMailClient() {
        LOGGER.warn(
                "`io.vertx.axle.ext.mail.MailClient` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.ext.mail.MailClient`");
        return io.vertx.axle.ext.mail.MailClient.newInstance(client);
    }

    @Singleton
    @Produces
    @Deprecated
    public io.vertx.reactivex.ext.mail.MailClient rxMailClient() {
        LOGGER.warn(
                "`io.vertx.reactivex.ext.mail.MailClient` is deprecated and will be removed in a future version - it is "
                        + "recommended to switch to `io.vertx.mutiny.ext.mail.MailClient`");
        return io.vertx.reactivex.ext.mail.MailClient.newInstance(client);
    }

    @PreDestroy
    public void stop() {
        client.close();
    }

    private MailClient mailClient(Vertx vertx, MailConfig config) {
        io.vertx.ext.mail.MailConfig cfg = toVertxMailConfig(config);
        return MailClient.createShared(vertx, cfg);
    }

    private io.vertx.ext.mail.MailConfig toVertxMailConfig(MailConfig config) {
        io.vertx.ext.mail.MailConfig cfg = new io.vertx.ext.mail.MailConfig();
        config.authMethods.ifPresent(cfg::setAuthMethods);
        cfg.setDisableEsmtp(config.disableEsmtp);
        cfg.setHostname(config.host);
        cfg.setKeepAlive(config.keepAlive);
        config.keyStore.ifPresent(cfg::setKeyStore);
        config.keyStorePassword.ifPresent(cfg::setKeyStorePassword);
        config.login.ifPresent(s -> cfg.setLogin(LoginOption.valueOf(s.toUpperCase())));
        config.maxPoolSize.ifPresent(cfg::setMaxPoolSize);
        config.ownHostName.ifPresent(cfg::setOwnHostname);
        config.username.ifPresent(cfg::setUsername);
        config.password.ifPresent(cfg::setPassword);
        config.port.ifPresent(cfg::setPort);
        cfg.setSsl(config.ssl);
        config.startTLS.ifPresent(s -> cfg.setStarttls(StartTLSOptions.valueOf(s.toUpperCase())));
        cfg.setTrustAll(config.trustAll);
        return cfg;
    }

}
