package io.quarkus.mailer.runtime;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MailerTruststoreTest extends FakeSmtpTestBase {

    @Test
    public void sendMailWithCorrectTrustStore() {
        MailConfig config = getDefaultConfig();
        config.ssl = true;
        config.truststore.password = Optional.of("password");
        config.truststore.paths = Optional.of(Collections.singletonList(CLIENT_JKS));

        MutinyMailerImpl mailer = getMailer(config);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void sendMailWithCorrectButDeprecatedTrustStore() {
        MailConfig config = getDefaultConfig();
        config.ssl = true;
        config.keyStorePassword = Optional.of("password");
        config.keyStore = Optional.of(CLIENT_JKS);

        MutinyMailerImpl mailer = getMailer(config);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithValidCertsButWrongHost() {
        MailConfig config = getDefaultConfig();
        config.host = "127.0.0.1"; // Expecting localhost.
        config.ssl = true;
        config.truststore.password = Optional.of("password");
        config.truststore.paths = Optional.of(Collections.singletonList(CLIENT_JKS));

        startServer(SERVER_JKS);
        MutinyMailerImpl mailer = getMailer(config);
        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

    @Test
    public void sendMailWithTrustAll() {
        MailConfig config = getDefaultConfig();
        config.ssl = true;
        config.trustAll = Optional.of(true);
        MutinyMailerImpl mailer = getMailer(config);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithGlobalTrustAll() {
        MailConfig config = getDefaultConfig();
        config.ssl = true;
        MutinyMailerImpl mailer = getMailer(config, true);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithoutTrustStore() {
        MailConfig config = getDefaultConfig();
        config.ssl = true;

        startServer(SERVER_JKS);
        MutinyMailerImpl mailer = getMailer(config);
        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

}
