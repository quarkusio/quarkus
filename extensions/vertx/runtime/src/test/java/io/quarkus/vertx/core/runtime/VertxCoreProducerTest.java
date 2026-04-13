package io.quarkus.vertx.core.runtime;

import java.time.Duration;
import java.util.Collections;
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
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxCustomizer;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.impl.SysProps;

public class VertxCoreProducerTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() {
        recorder = new VertxCoreRecorder(new RuntimeValue<>(), new RuntimeValue<>(), new RuntimeValue<>());
    }

    @AfterEach
    public void tearDown() {
        recorder.destroy();
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

        VertxCustomizer customizers = new VertxCustomizer(Collections.emptyList(), List.of(
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

        VertxCoreRecorder.initialize(configuration, customizers, new DefaultThreadPoolConfig(), null, LaunchMode.TEST,
                List.of());
    }

    @Test
    public void shouldInvokeCustomizers() {
        final AtomicBoolean called = new AtomicBoolean(false);
        VertxCustomizer customizers = new VertxCustomizer(Collections.emptyList(), List.of(
                new Consumer<VertxOptions>() {
                    @Override
                    public void accept(VertxOptions vertxOptions) {
                        called.set(true);
                    }
                }));
        Vertx v = VertxCoreRecorder.initialize(new DefaultVertxConfiguration(), customizers, new DefaultThreadPoolConfig(),
                null, LaunchMode.TEST, List.of());
        Assertions.assertTrue(called.get(), "Customizer should get called during initialization");
    }

    @Test
    public void vertxCacheDirectoryBySystemProperty() {
        final String cacheDir = System.getProperty("user.dir");
        try {
            System.setProperty(SysProps.FILE_CACHE_DIR.name, cacheDir);
            VertxCustomizer customizers = new VertxCustomizer(Collections.emptyList(), List.of(
                    vertxOptions -> {
                        Assertions.assertNotNull(vertxOptions.getFileSystemOptions());
                        Assertions.assertEquals(cacheDir, vertxOptions.getFileSystemOptions().getFileCacheDir());
                    }));
            VertxCoreRecorder.initialize(new DefaultVertxConfiguration(), customizers, new DefaultThreadPoolConfig(),
                    null, LaunchMode.TEST, List.of());
        } finally {
            System.clearProperty(SysProps.FILE_CACHE_DIR.name);
        }
    }

    static class DefaultVertxConfiguration implements VertxConfiguration {
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
        public Duration blockedThreadCheckInterval() {
            return Duration.ofSeconds(1);
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
