package io.quarkus.mailer.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.mutiny.core.Vertx;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "mailer-certs", formats = Format.PKCS12, password = "password") })
public class FakeSmtpTestBase {

    protected static final int FAKE_SMTP_PORT = 1465;
    protected static final String SERVER_JKS = "target/certs/mailer-certs-keystore.p12";
    protected static final String CLIENT_TRUSTSTORE = "target/certs/mailer-certs-truststore.p12";

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

    protected ReactiveMailer getMailer(MailersRuntimeConfig config, boolean trustAll) {
        Mailers mailers = new Mailers(vertx.getDelegate(), vertx, config, LaunchMode.NORMAL,
                new MailerSupport(true, Set.of()),
                new TlsConfigurationRegistry() {

                    @Override
                    public Optional<TlsConfiguration> get(String name) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public Optional<TlsConfiguration> getDefault() {
                        return Optional.of(new BaseTlsConfiguration() {
                            @Override
                            public boolean isTrustAll() {
                                return trustAll;
                            }

                            @Override
                            public String getName() {
                                return "test";
                            }
                        });
                    }

                    @Override
                    public void register(String name, TlsConfiguration configuration) {
                        throw new UnsupportedOperationException();
                    }
                }, null);
        return mailers.reactiveMailerFromName(Mailers.DEFAULT_MAILER_NAME);
    }

    protected ReactiveMailer getMailer(MailersRuntimeConfig config, String confName, TlsConfiguration configuration) {
        Mailers mailers = new Mailers(vertx.getDelegate(), vertx, config, LaunchMode.NORMAL,
                new MailerSupport(true, Set.of()),
                new TlsConfigurationRegistry() {

                    @Override
                    public Optional<TlsConfiguration> get(String name) {
                        if (confName != null && confName.equals(name)) {
                            return Optional.of(configuration);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<TlsConfiguration> getDefault() {
                        if (confName == null) {
                            return Optional.of(configuration);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public void register(String name, TlsConfiguration configuration) {
                        throw new UnsupportedOperationException();
                    }
                }, null);
        return mailers.reactiveMailerFromName(Mailers.DEFAULT_MAILER_NAME);
    }

    static class DefaultMailersRuntimeConfig implements MailersRuntimeConfig {

        private DefaultMailerRuntimeConfig defaultMailerRuntimeConfig;

        DefaultMailersRuntimeConfig() {
            this(new DefaultMailerRuntimeConfig());
        }

        DefaultMailersRuntimeConfig(DefaultMailerRuntimeConfig defaultMailerRuntimeConfig) {
            this.defaultMailerRuntimeConfig = defaultMailerRuntimeConfig;
        }

        @Override
        public Map<String, MailerRuntimeConfig> mailers() {
            return Map.of(Mailers.DEFAULT_MAILER_NAME, defaultMailerRuntimeConfig);
        }

    }

    static class DefaultMailerRuntimeConfig implements MailerRuntimeConfig {

        @Override
        public Optional<String> from() {
            return Optional.of(FROM);
        }

        @Override
        public Optional<Boolean> mock() {
            return Optional.empty();
        }

        @Override
        public Optional<String> bounceAddress() {
            return Optional.empty();
        }

        @Override
        public String host() {
            return "localhost";
        }

        @Override
        public OptionalInt port() {
            return OptionalInt.of(FAKE_SMTP_PORT);
        }

        @Override
        public Optional<String> username() {
            return Optional.empty();
        }

        @Override
        public Optional<String> password() {
            return Optional.empty();
        }

        @Override
        public Optional<String> tlsConfigurationName() {
            return Optional.empty();
        }

        @Override
        public boolean ssl() {
            return false;
        }

        @Override
        public Optional<Boolean> tls() {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> trustAll() {
            return Optional.empty();
        }

        @Override
        public int maxPoolSize() {
            return 10;
        }

        @Override
        public Optional<String> ownHostName() {
            return Optional.empty();
        }

        @Override
        public boolean keepAlive() {
            return false;
        }

        @Override
        public boolean disableEsmtp() {
            return false;
        }

        @Override
        public String startTLS() {
            return "DISABLED";
        }

        @Override
        public DkimSignOptionsConfig dkim() {
            return null;
        }

        @Override
        public String login() {
            return "DISABLED";
        }

        @Override
        public Optional<String> authMethods() {
            return Optional.empty();
        }

        @Override
        public Optional<String> keyStore() {
            return Optional.empty();
        }

        @Override
        public Optional<String> keyStorePassword() {
            return Optional.empty();
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
                    return Optional.empty();
                }

                @Override
                public Optional<String> password() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public boolean multiPartOnly() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean allowRcptErrors() {
            return false;
        }

        @Override
        public boolean pipelining() {
            return false;
        }

        @Override
        public Duration poolCleanerPeriod() {
            return Duration.ofSeconds(1);
        }

        @Override
        public Duration keepAliveTimeout() {
            return Duration.ofMinutes(5);
        }

        @Override
        public NtlmConfig ntlm() {
            return null;
        }

        @Override
        public Optional<List<Pattern>> approvedRecipients() {
            return Optional.empty();
        }

        @Override
        public boolean logRejectedRecipients() {
            return false;
        }

        @Override
        public boolean logInvalidRecipients() {
            return false;
        }

    }

}
