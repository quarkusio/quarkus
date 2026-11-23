package io.quarkus.reactive.datasource.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;

class ReactivePoolUtilHostnameVerificationTest {

    @Test
    void hostnameVerificationNotSetUsesHttps() {
        assertThat(ReactivePoolUtil.toVertxHostnameVerificationAlgorithm(Optional.empty()))
                .isEqualTo(ReactivePoolUtil.HOSTNAME_VERIFICATION_DEFAULT_ALGO);
    }

    @Test
    void hostnameVerificationEmptyUsesHttps() {
        assertThat(ReactivePoolUtil.toVertxHostnameVerificationAlgorithm(Optional.of("")))
                .isEqualTo(ReactivePoolUtil.HOSTNAME_VERIFICATION_DEFAULT_ALGO);
    }

    @Test
    void hostnameVerificationValueIsPassedThrough() {
        assertThat(ReactivePoolUtil.toVertxHostnameVerificationAlgorithm(Optional.of("LDAPS"))).isEqualTo("LDAPS");
    }

    @Test
    void hostnameVerificationNoneDisablesVertxAlgorithm() {
        assertThat(ReactivePoolUtil.toVertxHostnameVerificationAlgorithm(Optional.of("NONE"))).isEqualTo("");
    }

    @Test
    void manualSslPropertiesAreFalseByDefault() {
        assertThat(ReactivePoolUtil.hasManualSslProperties(new TestConfig())).isFalse();
    }

    @Test
    void blankHostnameVerificationIsNotManualOverride() {
        TestConfig config = new TestConfig();
        config.hostnameVerificationAlgorithm = Optional.of("   ");
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isFalse();
    }

    @Test
    void emptyStringHostnameVerificationIsNotManualOverride() {
        TestConfig config = new TestConfig();
        config.hostnameVerificationAlgorithm = Optional.of("");
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isFalse();
    }

    @Test
    void explicitHostnameVerificationCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.hostnameVerificationAlgorithm = Optional.of("NONE");
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void trustAllCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.trustAll = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void pemTrustEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.pemTrustEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void pemTrustCertsCountAsManualOverride() {
        TestConfig config = new TestConfig();
        config.pemTrustCerts = Optional.of(List.of("server.crt"));
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void jksTrustEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.jksTrustEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void pfxTrustEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.pfxTrustEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void pemKeyEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.pemKeyEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void jksKeyEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.jksKeyEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    @Test
    void pfxKeyEnabledCountsAsManualOverride() {
        TestConfig config = new TestConfig();
        config.pfxKeyEnabled = true;
        assertThat(ReactivePoolUtil.hasManualSslProperties(config)).isTrue();
    }

    private static final class TestConfig implements DataSourceReactiveRuntimeConfig {
        boolean trustAll;
        Optional<String> hostnameVerificationAlgorithm = Optional.empty();

        boolean pemTrustEnabled;
        Optional<List<String>> pemTrustCerts = Optional.empty();

        boolean jksTrustEnabled;
        boolean pfxTrustEnabled;

        boolean pemKeyEnabled;
        boolean jksKeyEnabled;
        boolean pfxKeyEnabled;

        @Override
        public Optional<Boolean> cachePreparedStatements() {
            return Optional.empty();
        }

        @Override
        public OptionalInt preparedStatementCacheMaxSize() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt preparedStatementCacheSqlLimit() {
            return OptionalInt.empty();
        }

        @Override
        public Optional<List<String>> url() {
            return Optional.empty();
        }

        @Override
        public int maxSize() {
            return 20;
        }

        @Override
        public OptionalInt eventLoopSize() {
            return OptionalInt.empty();
        }

        @Override
        public boolean trustAll() {
            return trustAll;
        }

        @Override
        public PemTrustCertConfiguration trustCertificatePem() {
            return new PemTrustCertConfiguration() {
                @Override
                public boolean enabled() {
                    return pemTrustEnabled;
                }

                @Override
                public Optional<List<String>> certs() {
                    return pemTrustCerts;
                }
            };
        }

        @Override
        public JksConfiguration trustCertificateJks() {
            return new JksConfiguration() {
                @Override
                public boolean enabled() {
                    return jksTrustEnabled;
                }

                @Override
                public Optional<String> path() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> password() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public PfxConfiguration trustCertificatePfx() {
            return new PfxConfiguration() {
                @Override
                public boolean enabled() {
                    return pfxTrustEnabled;
                }

                @Override
                public Optional<String> path() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> password() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public PemKeyCertConfiguration keyCertificatePem() {
            return new PemKeyCertConfiguration() {
                @Override
                public boolean enabled() {
                    return pemKeyEnabled;
                }

                @Override
                public Optional<List<String>> keys() {
                    return Optional.empty();
                }

                @Override
                public Optional<List<String>> certs() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public JksConfiguration keyCertificateJks() {
            return new JksConfiguration() {
                @Override
                public boolean enabled() {
                    return jksKeyEnabled;
                }

                @Override
                public Optional<String> path() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> password() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public PfxConfiguration keyCertificatePfx() {
            return new PfxConfiguration() {
                @Override
                public boolean enabled() {
                    return pfxKeyEnabled;
                }

                @Override
                public Optional<String> path() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> password() {
                    return Optional.empty();
                }
            };
        }

        @Override
        public int reconnectAttempts() {
            return 0;
        }

        @Override
        public Duration reconnectInterval() {
            return Duration.ofSeconds(1);
        }

        @Override
        public Optional<String> hostnameVerificationAlgorithm() {
            return hostnameVerificationAlgorithm;
        }

        @Override
        public Optional<String> tlsConfigurationName() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectionTimeout() {
            return Optional.empty();
        }

        @Override
        public OptionalInt maxWaitQueueSize() {
            return OptionalInt.empty();
        }

        @Override
        public Optional<Duration> poolCleanerPeriod() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> idleTimeout() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> maxLifetime() {
            return Optional.empty();
        }

        @Override
        public boolean tcpKeepAlive() {
            return false;
        }

        @Override
        public Optional<Duration> tcpKeepAliveIdle() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> tcpKeepAliveInterval() {
            return Optional.empty();
        }

        @Override
        public OptionalInt tcpKeepAliveCount() {
            return OptionalInt.empty();
        }

        @Override
        public boolean shared() {
            return false;
        }

        @Override
        public Optional<String> name() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> additionalProperties() {
            return Map.of();
        }
    }
}
