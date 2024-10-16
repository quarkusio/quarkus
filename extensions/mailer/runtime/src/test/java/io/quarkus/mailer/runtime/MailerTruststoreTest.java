package io.quarkus.mailer.runtime;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.reactive.ReactiveMailer;

public class MailerTruststoreTest extends FakeSmtpTestBase {

    @Test
    public void sendMailWithCorrectTrustStore() {
        MailersRuntimeConfig mailersConfig = getDefaultConfig();
        mailersConfig.defaultMailer.ssl = true;
        mailersConfig.defaultMailer.truststore.password = Optional.of("password");
        mailersConfig.defaultMailer.truststore.paths = Optional.of(Collections.singletonList(CLIENT_TRUSTSTORE));

        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void sendMailWithCorrectButDeprecatedTrustStore() {
        MailersRuntimeConfig mailersConfig = getDefaultConfig();
        mailersConfig.defaultMailer.ssl = true;
        mailersConfig.defaultMailer.keyStorePassword = Optional.of("password");
        mailersConfig.defaultMailer.keyStore = Optional.of(CLIENT_TRUSTSTORE);

        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithTrustAll() {
        MailersRuntimeConfig mailersConfig = getDefaultConfig();
        mailersConfig.defaultMailer.ssl = true;
        mailersConfig.defaultMailer.trustAll = Optional.of(true);
        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithGlobalTrustAll() {
        MailersRuntimeConfig mailersConfig = getDefaultConfig();
        mailersConfig.defaultMailer.ssl = true;
        ReactiveMailer mailer = getMailer(mailersConfig, true);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithoutTrustStore() {
        MailersRuntimeConfig mailersConfig = getDefaultConfig();
        mailersConfig.defaultMailer.ssl = true;

        startServer(SERVER_JKS);
        ReactiveMailer mailer = getMailer(mailersConfig);
        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

}
