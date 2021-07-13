package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.mailer.Mail;
import io.quarkus.runtime.TlsConfig;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.mail.MailClient;

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

    protected MutinyMailerImpl getMailer(MailConfig config) {
        return getMailer(config, false);
    }

    protected MutinyMailerImpl getMailer(MailConfig config, boolean globalTrustAll) {
        TlsConfig tlsConfig = new TlsConfig();
        if (globalTrustAll) {
            tlsConfig.trustAll = true;
        }
        MailClientProducer producer = new MailClientProducer(vertx.getDelegate(), config, tlsConfig);
        MailClient client = producer.mutinyClient();
        MutinyMailerImpl mailer = new MutinyMailerImpl();
        mailer.vertx = vertx;
        mailer.mailerSupport = new MailerSupport(FROM, null, false);
        mailer.client = client;
        return mailer;
    }

    protected MailConfig getDefaultConfig() {
        MailConfig config = new MailConfig();
        config.host = "localhost";
        config.port = OptionalInt.of(FAKE_SMTP_PORT);
        config.startTLS = "DISABLED";
        config.login = "DISABLED";
        config.ssl = false;
        config.authMethods = Optional.empty();
        config.maxPoolSize = 10;
        config.ownHostName = Optional.empty();
        config.username = Optional.empty();
        config.password = Optional.empty();
        config.poolCleanerPeriod = Duration.ofSeconds(1);
        config.keepAlive = true;
        config.keepAliveTimeout = Duration.ofMinutes(5);
        config.trustAll = Optional.empty();
        config.keyStore = Optional.empty();
        config.keyStorePassword = Optional.empty();
        config.truststore = new TrustStoreConfig();
        config.truststore.paths = Optional.empty();
        config.truststore.password = Optional.empty();
        config.truststore.type = Optional.empty();
        return config;
    }

}
