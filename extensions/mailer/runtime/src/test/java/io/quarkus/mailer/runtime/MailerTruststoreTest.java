package io.quarkus.mailer.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.reactive.ReactiveMailer;

public class MailerTruststoreTest extends FakeSmtpTestBase {

    @Test
    public void sendMailWithCorrectTrustStore() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public boolean ssl() {
                return true;
            }

            @Override
            public TrustStoreConfig truststore() {
                return new TrustStoreConfig() {

                    @Override
                    public Optional<String> type() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<List<String>> paths() {
                        return Optional.of(List.of(CLIENT_TRUSTSTORE));
                    }

                    @Override
                    public Optional<String> password() {
                        return Optional.of("password");
                    }
                };
            }
        });

        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void sendMailWithCorrectButDeprecatedTrustStore() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public boolean ssl() {
                return true;
            }

            @Override
            public Optional<String> keyStorePassword() {
                return Optional.of("password");
            }

            @Override
            public Optional<String> keyStore() {
                return Optional.of(CLIENT_TRUSTSTORE);
            }
        });

        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithTrustAll() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public boolean ssl() {
                return true;
            }

            @Override
            public Optional<Boolean> trustAll() {
                return Optional.of(true);
            }
        });

        ReactiveMailer mailer = getMailer(mailersConfig);
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithGlobalTrustAll() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public boolean ssl() {
                return true;
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
            public boolean ssl() {
                return true;
            }
        });

        startServer(SERVER_JKS);
        ReactiveMailer mailer = getMailer(mailersConfig);
        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

}
