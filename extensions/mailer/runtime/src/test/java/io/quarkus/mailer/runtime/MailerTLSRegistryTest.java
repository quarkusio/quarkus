package io.quarkus.mailer.runtime;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLHandshakeException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.tls.BaseTlsConfiguration;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.TrustOptions;

public class MailerTLSRegistryTest extends FakeSmtpTestBase {

    @Test
    public void sendMailWithCorrectTrustStore() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.of("my-mailer");
            }

            @Override
            public Optional<Boolean> tls() {
                return Optional.of(true);
            }
        });
        ReactiveMailer mailer = getMailer(mailersConfig, "my-mailer", new BaseTlsConfiguration() {

            @Override
            public TrustOptions getTrustStoreOptions() {
                JksOptions jksOptions = new JksOptions();
                jksOptions.setPath("target/certs/mailer-certs-truststore.p12");
                jksOptions.setPassword("password");
                return jksOptions;
            }

            @Override
            public String getName() {
                return "test";
            }

        });
        startServer(new File("target/certs/mailer-certs-keystore.p12").getAbsolutePath());
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithDefaultTrustAll() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig();
        ReactiveMailer mailer = getMailer(mailersConfig, null, new BaseTlsConfiguration() {

            @Override
            public boolean isTrustAll() {
                return true;
            }

            @Override
            public String getName() {
                return "test";
            }
        });
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithNamedTrustAll() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.of("my-mailer");
            }
        });
        ReactiveMailer mailer = getMailer(mailersConfig, "my-mailer", new BaseTlsConfiguration() {

            @Override
            public boolean isTrustAll() {
                return true;
            }

            @Override
            public String getName() {
                return "test";
            }
        });
        startServer(SERVER_JKS);
        mailer.send(getMail()).await().indefinitely();
    }

    @Test
    public void sendMailWithoutTrustStore() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.of("my-mailer");
            }

            @Override
            public Optional<Boolean> tls() {
                return Optional.of(true);
            }
        });
        startServer(SERVER_JKS);
        ReactiveMailer mailer = getMailer(mailersConfig, "my-mailer", new BaseTlsConfiguration() {
            @Override
            public String getName() {
                return "test";
            }
        });

        Assertions.assertThatThrownBy(() -> mailer.send(getMail()).await().indefinitely())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

    @Test
    public void testWithWrongTlsName() {
        MailersRuntimeConfig mailersConfig = new DefaultMailersRuntimeConfig(new DefaultMailerRuntimeConfig() {
            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.of("missing-mailer-configuration");
            }
        });
        Assertions.assertThatThrownBy(() -> getMailer(mailersConfig, "my-mailer", new BaseTlsConfiguration() {

            @Override
            public TrustOptions getTrustStoreOptions() {
                JksOptions jksOptions = new JksOptions();
                jksOptions.setPath("target/certs/mailer-certs-truststore.p12");
                jksOptions.setPassword("password");
                return jksOptions;
            }

            @Override
            public String getName() {
                return "test";
            }

        })).hasMessageContaining("missing-mailer-configuration");
    }

}
