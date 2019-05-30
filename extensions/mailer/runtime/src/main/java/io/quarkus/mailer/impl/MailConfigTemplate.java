package io.quarkus.mailer.impl;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.vertx.core.Vertx;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.StartTLSOptions;

@Template
public class MailConfigTemplate {

    private static volatile MailClient client;

    public RuntimeValue<MailClient> configureTheClient(RuntimeValue<Vertx> vertx, BeanContainer container,
            MailConfig config, LaunchMode launchMode, ShutdownContext shutdown) {

        initialize(vertx.getValue(), config);

        MailClientProducer producer = container.instance(MailClientProducer.class);
        producer.initialize(client);

        if (!launchMode.isDevOrTest()) {
            shutdown.addShutdownTask(this::close);
        }
        return new RuntimeValue<>(client);
    }

    public RuntimeValue<ReactiveMailerImpl> configureTheMailer(BeanContainer container, MailConfig config) {

        ReactiveMailerImpl mailer = container.instance(ReactiveMailerImpl.class);
        mailer.configure(config.from, config.bounceAddress, config.mock);

        return new RuntimeValue<>(mailer);
    }

    void initialize(Vertx vertx, MailConfig config) {
        if (client != null) {
            // Already configured
            return;
        }
        io.vertx.ext.mail.MailConfig cfg = toVertxMailConfig(config);
        client = MailClient.createNonShared(vertx, cfg);
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

    void close() {
        if (client != null) {
            client.close();
        }
    }
}
