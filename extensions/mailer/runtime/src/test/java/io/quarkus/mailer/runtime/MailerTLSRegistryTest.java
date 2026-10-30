package io.quarkus.mailer.runtime;

import java.io.File;
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
        MailersRuntimeConfig mailersConfig = mailersRuntimeConfig(defaultMailerRuntimeConfig()
                .withOptional(MailerRuntimeConfig::tlsConfigurationName, "my-mailer")
                .withOptional(MailerRuntimeConfig::tls, true));
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
        MailersRuntimeConfig mailersConfig = mailersRuntimeConfig(defaultMailerRuntimeConfig());
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
        MailersRuntimeConfig mailersConfig = mailersRuntimeConfig(defaultMailerRuntimeConfig()
                .withOptional(MailerRuntimeConfig::tlsConfigurationName, "my-mailer"));
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
        MailersRuntimeConfig mailersConfig = mailersRuntimeConfig(defaultMailerRuntimeConfig()
                .withOptional(MailerRuntimeConfig::tlsConfigurationName, "my-mailer")
                .withOptional(MailerRuntimeConfig::tls, true));
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
        MailersRuntimeConfig mailersConfig = mailersRuntimeConfig(defaultMailerRuntimeConfig()
                .withOptional(MailerRuntimeConfig::tlsConfigurationName, "missing-mailer-configuration"));
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
