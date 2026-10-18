package io.quarkus.mailer.runtime;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.reactive.ReactiveMailer;

public class MailerTruststoreTest extends FakeSmtpTestBase {

    @Test
    public void sendMailWithGlobalTrustAll() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<Boolean> tls() {
                return Optional.of(true);
            }
        });
        ReactiveMailer mailer = getMailer(mailersConfig, true);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithoutTrustStore() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<Boolean> tls() {
                return Optional.of(true);
            }
        });

        startServer(SERVER_JKS);
        ReactiveMailer mailer = getMailer(mailersConfig);
        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

}
