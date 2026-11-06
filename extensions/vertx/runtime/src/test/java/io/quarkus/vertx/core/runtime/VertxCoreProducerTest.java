package io.quarkus.vertx.core.runtime;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxCustomizer;
import io.quarkus.vertx.core.runtime.config.AddressResolverConfiguration;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.smallrye.config.ConfigInstanceBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.impl.SysProps;

public class VertxCoreProducerTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() {
        recorder = new VertxCoreRecorder(null, new RuntimeValue<>(), new RuntimeValue<>(), new RuntimeValue<>());
    }

    @AfterEach
    public void tearDown() {
        recorder.destroy();
    }

    @Test
    public void shouldConfigureAddressResolver() {
        VertxConfiguration configuration = vertxConfiguration()
                .with(VertxConfiguration::resolver, addressResolverConfiguration()
                        .with(AddressResolverConfiguration::queryTimeout, Duration.ofMillis(200L))
                        .with(AddressResolverConfiguration::maxQueries, 2)
                        .with(AddressResolverConfiguration::cacheNegativeTimeToLive, Duration.ofSeconds(1))
                        .with(AddressResolverConfiguration::cacheMinTimeToLive, Duration.ofSeconds(0))
                        .with(AddressResolverConfiguration::cacheMaxTimeToLive, Duration.ofSeconds(3))
                        .build())
                .build();

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

        VertxCoreRecorder.initialize(configuration, customizers, threadPoolConfig().build(), null, LaunchMode.TEST,
                List.of(), List.of());
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
        Vertx v = VertxCoreRecorder.initialize(vertxConfiguration().build(), customizers, threadPoolConfig().build(),
                null, LaunchMode.TEST, List.of(), List.of());
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
            VertxCoreRecorder.initialize(vertxConfiguration().build(), customizers, threadPoolConfig().build(),
                    null, LaunchMode.TEST, List.of(), List.of());
        } finally {
            System.clearProperty(SysProps.FILE_CACHE_DIR.name);
        }
    }

    static ConfigInstanceBuilder<VertxConfiguration> vertxConfiguration() {
        // differs from the defaults declared in `VertxConfiguration`
        return ConfigInstanceBuilder.forInterface(VertxConfiguration.class)
                .with(VertxConfiguration::maxWorkerExecuteTime, Duration.ofSeconds(1))
                .with(VertxConfiguration::resolver, addressResolverConfiguration().build());
    }

    static ConfigInstanceBuilder<AddressResolverConfiguration> addressResolverConfiguration() {
        // differs from the defaults declared in `AddressResolverConfiguration`
        return ConfigInstanceBuilder.forInterface(AddressResolverConfiguration.class)
                .with(AddressResolverConfiguration::rdFlag, false)
                .with(AddressResolverConfiguration::ndots, 0);
    }

    static ConfigInstanceBuilder<ThreadPoolConfig> threadPoolConfig() {
        // differs from the defaults declared in `ThreadPoolConfig`
        return ConfigInstanceBuilder.forInterface(ThreadPoolConfig.class)
                .with(ThreadPoolConfig::coreThreads, 0)
                .with(ThreadPoolConfig::shutdownCheckInterval, Optional.<Duration> empty());
    }
}
