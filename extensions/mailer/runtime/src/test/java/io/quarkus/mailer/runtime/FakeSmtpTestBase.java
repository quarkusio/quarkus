package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.TlsConfig;
import io.vertx.mutiny.core.Vertx;

public class FakeSmtpTestBase {

    protected static final int FAKE_SMTP_PORT = 1465;
    protected static final String SERVER_JKS = "certs/server2.jks";
    protected static final String CLIENT_JKS = "certs/client.jks";

    protected static final String FROM = "test@test.org";
    protected static final String TO = "foo@quarkus.io";

    protected Vertx vertx;
    protected FakeSmtpServer smtpServer;

    @BeforeEach
    void startVertx() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void stopVertx() {
        vertx.close().await().indefinitely();
    }

    protected void startServer(String keystore) {
        smtpServer = new FakeSmtpServer(vertx, true, keystore);
    }

    protected Mail getMail() {
        return new Mail().setFrom(FROM).addTo(TO).setSubject("Subject").setText("Message");
    }

    protected ReactiveMailer getMailer(MailersRuntimeConfig config) {
        return getMailer(config, false);
    }

    protected ReactiveMailer getMailer(MailersRuntimeConfig config, boolean globalTrustAll) {
        TlsConfig globalTlsConfig = new TlsConfig();
        if (globalTrustAll) {
            globalTlsConfig.trustAll = true;
        }

        Mailers mailers = new Mailers(vertx.getDelegate(), vertx, config, globalTlsConfig, LaunchMode.NORMAL,
                new MailerSupport(true, Set.of()));

        return mailers.reactiveMailerFromName(Mailers.DEFAULT_MAILER_NAME);
    }

    protected MailersRuntimeConfig getDefaultConfig() {
        MailersRuntimeConfig mailersConfig = new MailersRuntimeConfig();
        mailersConfig.defaultMailer = new MailerRuntimeConfig();
        mailersConfig.defaultMailer.from = Optional.of(FROM);
        mailersConfig.defaultMailer.host = "localhost";
        mailersConfig.defaultMailer.port = OptionalInt.of(FAKE_SMTP_PORT);
        mailersConfig.defaultMailer.startTLS = "DISABLED";
        mailersConfig.defaultMailer.login = "DISABLED";
        mailersConfig.defaultMailer.ssl = false;
        mailersConfig.defaultMailer.authMethods = Optional.empty();
        mailersConfig.defaultMailer.maxPoolSize = 10;
        mailersConfig.defaultMailer.ownHostName = Optional.empty();
        mailersConfig.defaultMailer.username = Optional.empty();
        mailersConfig.defaultMailer.password = Optional.empty();
        mailersConfig.defaultMailer.poolCleanerPeriod = Duration.ofSeconds(1);
        mailersConfig.defaultMailer.keepAlive = true;
        mailersConfig.defaultMailer.keepAliveTimeout = Duration.ofMinutes(5);
        mailersConfig.defaultMailer.trustAll = Optional.empty();
        mailersConfig.defaultMailer.keyStore = Optional.empty();
        mailersConfig.defaultMailer.keyStorePassword = Optional.empty();
        mailersConfig.defaultMailer.truststore = new TrustStoreConfig();
        mailersConfig.defaultMailer.truststore.paths = Optional.empty();
        mailersConfig.defaultMailer.truststore.password = Optional.empty();
        mailersConfig.defaultMailer.truststore.type = Optional.empty();

        return mailersConfig;
    }

}
