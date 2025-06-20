package io.quarkus.vertx.core.runtime;

import static io.vertx.core.file.impl.FileResolverImpl.CACHE_DIR_BASE_PROP_NAME;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxOptionsCustomizer;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.ClusterConfiguration;
import io.quarkus.vertx.core.runtime.config.EventBusConfiguration;
import io.quarkus.vertx.core.runtime.config.JksConfiguration;
import io.quarkus.vertx.core.runtime.config.PemKeyCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PemTrustCertConfiguration;
import io.quarkus.vertx.core.runtime.config.PfxConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;

public class VertxCoreProducerTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() throws Exception {
        recorder = new VertxCoreRecorder(new RuntimeValue<>(), new RuntimeValue<>());
    }

    @AfterEach
    public void tearDown() throws Exception {
        recorder.destroy();
    }

    @Test
    public void shouldEnableClustering() {
        VertxConfiguration configuration = new DefaultVertxConfiguration() {

            @Override
            public ClusterConfiguration cluster() {
                return new ClusterConfiguration() {

                    @Override
                    public OptionalInt publicPort() {
                        return OptionalInt.empty();
                    }

                    @Override
                    public Optional<String> publicHost() {
                        return Optional.empty();
                    }

                    @Override
                    public OptionalInt port() {
                        return OptionalInt.empty();
                    }

                    @Override
                    public Duration pingReplyInterval() {
                        return Duration.ofSeconds(2);
                    }

                    @Override
                    public Duration pingInterval() {
                        return Duration.ofSeconds(2);
                    }

                    @Override
                    public String host() {
                        return "localhost";
                    }

                    @Override
                    public boolean clustered() {
                        return true;
                    }
                };
            }

            @Override
            public int workerPoolSize() {
                return 10;
            }

            @Override
            public Duration warningExceptionTime() {
                return Duration.ofSeconds(1);
            }
        };

        try {

            VertxCoreRecorder.initialize(configuration, null, new DefaultThreadPoolConfig(), null, LaunchMode.TEST);
            Assertions.fail("It should not have a cluster manager on the classpath, and so fail the creation");
        } catch (IllegalStateException e) {
            Assertions.assertTrue(e.getMessage().contains("No ClusterManagerFactory"),
                    "The message should contain ''. Message: " + e.getMessage());
        }
    }

    @Test
    public void shouldConfigureAddressResolver() {
        VertxConfiguration configuration = new DefaultVertxConfiguration() {
            @Override
            public AddressResolverConfiguration resolver() {
                return new AddressResolverConfiguration() {

                    @Override
                    public Duration queryTimeout() {
                        return Duration.ofMillis(200L);
                    }

                    @Override
                    public int maxQueries() {
                        return 2;
                    }

                    @Override
                    public int cacheNegativeTimeToLive() {
                        return 1;
                    }

                    @Override
                    public int cacheMinTimeToLive() {
                        return 0;
                    }

                    @Override
                    public int cacheMaxTimeToLive() {
                        return 3;
                    }

                    @Override
                    public Optional<String> hostsPath() {
                        return Optional.empty();
                    }

                    @Override
                    public int hostRefreshPeriod() {
                        return 0;
                    }

                    @Override
                    public Optional<List<String>> servers() {
                        return Optional.empty();
                    }

                    @Override
                    public boolean optResourceEnabled() {
                        return false;
                    }

                    @Override
                    public boolean rdFlag() {
                        return false;
                    }

                    @Override
                    public Optional<List<String>> searchDomains() {
                        return Optional.empty();
                    }

                    @Override
                    public int ndots() {
                        return 0;
                    }

                    @Override
                    public Optional<Boolean> rotateServers() {
                        return Optional.empty();
                    }

                    @Override
                    public boolean roundRobinInetAddress() {
                        return false;
                    }
                };
            }
        };

        VertxOptionsCustomizer customizers = new VertxOptionsCustomizer(Arrays.asList(
                new Consumer<VertxOptions>() {
                    @Override
                    public void accept(VertxOptions vertxOptions) {
                        Assertions.assertEquals(3, vertxOptions.getAddressResolverOptions().getCacheMaxTimeToLive());
                        Assertions.assertEquals(
                                AddressResolverOptions.DEFAULT_CACHE_MIN_TIME_TO_LIVE,
                                vertxOptions.getAddressResolverOptions().getCacheMinTimeToLive());
                        Assertions.assertEquals(1, vertxOptions.getAddressResolverOptions().getCacheNegativeTimeToLive());
                        Assertions.assertEquals(2, vertxOptions.getAddressResolverOptions().getMaxQueries());
                        Assertions.assertEquals(200L, vertxOptions.getAddressResolverOptions().getQueryTimeout());
                    }
                }));

        VertxCoreRecorder.initialize(configuration, customizers, new DefaultThreadPoolConfig(), null, LaunchMode.TEST);
    }

    @Test
    public void shouldInvokeCustomizers() {
        final AtomicBoolean called = new AtomicBoolean(false);
        VertxOptionsCustomizer customizers = new VertxOptionsCustomizer(Arrays.asList(
                new Consumer<VertxOptions>() {
                    @Override
                    public void accept(VertxOptions vertxOptions) {
                        called.set(true);
                    }
                }));
        Vertx v = VertxCoreRecorder.initialize(new DefaultVertxConfiguration(), customizers, new DefaultThreadPoolConfig(),
                null,
                LaunchMode.TEST);
        Assertions.assertTrue(called.get(), "Customizer should get called during initialization");
    }

    @Test
    public void vertxCacheDirectoryBySystemProperty() {
        final String cacheDir = System.getProperty("user.dir");
        try {
            System.setProperty(CACHE_DIR_BASE_PROP_NAME, cacheDir);
            VertxOptionsCustomizer customizers = new VertxOptionsCustomizer(List.of(
                    vertxOptions -> {
                        Assertions.assertNotNull(vertxOptions.getFileSystemOptions());
                        Assertions.assertEquals(cacheDir, vertxOptions.getFileSystemOptions().getFileCacheDir());
                    }));
            VertxCoreRecorder.initialize(new DefaultVertxConfiguration(), customizers, new DefaultThreadPoolConfig(),
                    null,
                    LaunchMode.TEST);
        } finally {
            System.clearProperty(CACHE_DIR_BASE_PROP_NAME);
        }
    }

    private static class DefaultVertxConfiguration implements VertxConfiguration {
        @Override
        public boolean caching() {
            return true;
        }

        @Override
        public Optional<String> cacheDirectory() {
            return Optional.empty();
        }

        @Override
        public boolean classpathResolving() {
            return true;
        }

        @Override
        public OptionalInt eventLoopsPoolSize() {
            return OptionalInt.empty();
        }

        @Override
        public Duration maxEventLoopExecuteTime() {
            return Duration.ofSeconds(2);
        }

        @Override
        public Duration warningExceptionTime() {
            return Duration.ofSeconds(2);
        }

        @Override
        public int workerPoolSize() {
            return 20;
        }

        @Override
        public Duration maxWorkerExecuteTime() {
            return Duration.ofSeconds(1);
        }

        @Override
        public int internalBlockingPoolSize() {
            return 20;
        }

        @Override
        public OptionalInt queueSize() {
            return OptionalInt.empty();
        }

        @Override
        public float growthResistance() {
            return 0;
        }

        @Override
        public Duration keepAliveTime() {
            return Duration.ofSeconds(30);
        }

        @Override
        public boolean prefill() {
            return false;
        }

        @Override
        public boolean useAsyncDNS() {
            return false;
        }

        @Override
        public EventBusConfiguration eventbus() {
            return new EventBusConfiguration() {

                @Override
                public PfxConfiguration trustCertificatePfx() {
                    return new PfxConfiguration() {
                        @Override
                        public Optional<String> path() {
                            return Optional.empty();
                        }

                        @Override
                        public Optional<String> password() {
                            return Optional.empty();
                        }

                        @Override
                        public boolean enabled() {
                            return false;
                        }
                    };
                }

                @Override
                public PemTrustCertConfiguration trustCertificatePem() {
                    return new PemTrustCertConfiguration() {

                        @Override
                        public boolean enabled() {
                            return false;
                        }

                        @Override
                        public Optional<List<String>> certs() {
                            return Optional.empty();
                        }
                    };
                }

                @Override
                public JksConfiguration trustCertificateJks() {
                    return new JksConfiguration() {

                        @Override
                        public Optional<String> path() {
                            return Optional.empty();
                        }

                        @Override
                        public Optional<String> password() {
                            return Optional.empty();
                        }

                        @Override
                        public boolean enabled() {
                            return false;
                        }
                    };
                }

                @Override
                public boolean trustAll() {
                    return false;
                }

                @Override
                public OptionalInt trafficClass() {
                    return OptionalInt.empty();
                }

                @Override
                public boolean tcpNoDelay() {
                    return false;
                }

                @Override
                public boolean tcpKeepAlive() {
                    return false;
                }

                @Override
                public boolean ssl() {
                    return false;
                }

                @Override
                public OptionalInt soLinger() {
                    return OptionalInt.empty();
                }

                @Override
                public OptionalInt sendBufferSize() {
                    return OptionalInt.empty();
                }

                @Override
                public boolean reusePort() {
                    return false;
                }

                @Override
                public boolean reuseAddress() {
                    return false;
                }

                @Override
                public Duration reconnectInterval() {
                    return Duration.ofSeconds(1);
                }

                @Override
                public int reconnectAttempts() {
                    return 0;
                }

                @Override
                public OptionalInt receiveBufferSize() {
                    return OptionalInt.empty();
                }

                @Override
                public PfxConfiguration keyCertificatePfx() {
                    return new PfxConfiguration() {

                        @Override
                        public Optional<String> path() {
                            return Optional.empty();
                        }

                        @Override
                        public Optional<String> password() {
                            return Optional.empty();
                        }

                        @Override
                        public boolean enabled() {
                            return false;
                        }
                    };
                }

                @Override
                public PemKeyCertConfiguration keyCertificatePem() {
                    return new PemKeyCertConfiguration() {

                        @Override
                        public Optional<List<String>> keys() {
                            return Optional.empty();
                        }

                        @Override
                        public boolean enabled() {
                            return false;
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
                        public Optional<String> path() {
                            return Optional.empty();
                        }

                        @Override
                        public Optional<String> password() {
                            return Optional.empty();
                        }

                        @Override
                        public boolean enabled() {
                            return false;
                        }
                    };
                }

                @Override
                public Optional<Duration> idleTimeout() {
                    return Optional.empty();
                }

                @Override
                public Duration connectTimeout() {
                    return Duration.ofSeconds(60);
                }

                @Override
                public String clientAuth() {
                    return "NONE";
                }

                @Override
                public OptionalInt acceptBacklog() {
                    return OptionalInt.empty();
                }
            };
        }

        @Override
        public ClusterConfiguration cluster() {
            return new ClusterConfiguration() {

                @Override
                public OptionalInt publicPort() {
                    return OptionalInt.empty();
                }

                @Override
                public Optional<String> publicHost() {
                    return Optional.empty();
                }

                @Override
                public OptionalInt port() {
                    return OptionalInt.empty();
                }

                @Override
                public Duration pingReplyInterval() {
                    return Duration.ofSeconds(20);
                }

                @Override
                public Duration pingInterval() {
                    return Duration.ofSeconds(20);
                }

                @Override
                public String host() {
                    return "localhost";
                }

                @Override
                public boolean clustered() {
                    return false;
                }
            };
        }

        @Override
        public AddressResolverConfiguration resolver() {
            return new AddressResolverConfiguration() {

                @Override
                public Duration queryTimeout() {
                    return Duration.ofSeconds(5);
                }

                @Override
                public int maxQueries() {
                    return 4;
                }

                @Override
                public int cacheNegativeTimeToLive() {
                    return 0;
                }

                @Override
                public int cacheMinTimeToLive() {
                    return 0;
                }

                @Override
                public int cacheMaxTimeToLive() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public Optional<String> hostsPath() {
                    return Optional.empty();
                }

                @Override
                public int hostRefreshPeriod() {
                    return 0;
                }

                @Override
                public Optional<List<String>> servers() {
                    return Optional.empty();
                }

                @Override
                public boolean optResourceEnabled() {
                    return false;
                }

                @Override
                public boolean rdFlag() {
                    return false;
                }

                @Override
                public Optional<List<String>> searchDomains() {
                    return Optional.empty();
                }

                @Override
                public int ndots() {
                    return 0;
                }

                @Override
                public Optional<Boolean> rotateServers() {
                    return Optional.empty();
                }

                @Override
                public boolean roundRobinInetAddress() {
                    return false;
                }
            };
        }

        @Override
        public boolean preferNativeTransport() {
            return false;
        }
    }

    static class DefaultThreadPoolConfig implements ThreadPoolConfig {
        @Override
        public int coreThreads() {
            return 0;
        }

        @Override
        public boolean prefill() {
            return true;
        }

        @Override
        public OptionalInt maxThreads() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt queueSize() {
            return OptionalInt.empty();
        }

        @Override
        public float growthResistance() {
            return 0;
        }

        @Override
        public Duration shutdownTimeout() {
            return DurationConverter.parseDuration("1M");
        }

        @Override
        public Duration shutdownInterrupt() {
            return DurationConverter.parseDuration("10");
        }

        @Override
        public Optional<Duration> shutdownCheckInterval() {
            return Optional.empty();
        }

        @Override
        public Duration keepAliveTime() {
            return DurationConverter.parseDuration("5");
        }
    }
}
