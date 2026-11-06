package io.quarkus.vertx.core.runtime;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxCustomizer;
import io.quarkus.vertx.core.runtime.config.NativeTransportMode;
import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.quarkus.vertx.core.runtime.config.VertxBuildTimeConfig;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.smallrye.config.ConfigInstanceBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.internal.VertxBootstrap;

public class VertxCoreRecorderConfigurationTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() {
        recorder = new VertxCoreRecorder(null, new RuntimeValue<>(), new RuntimeValue<>(), new RuntimeValue<>());
    }

    @AfterEach
    public void tearDown() {
        recorder.destroy();
    }

    private VertxOptions initializeAndCapture(VertxConfiguration config,
            ThreadPoolConfig threadPoolConfig) {
        AtomicReference<VertxOptions> captured = new AtomicReference<>();
        VertxCustomizer customizer = new VertxCustomizer(List.of(), List.of(captured::set));
        VertxCoreRecorder.initialize(config, customizer, threadPoolConfig, null, LaunchMode.TEST,
                List.of(), List.of());
        return captured.get();
    }

    private VertxOptions initializeAndCapture(VertxConfiguration config) {
        return initializeAndCapture(config, VertxCoreProducerTest.threadPoolConfig().build());
    }

    @Test
    public void shouldConfigureEventLoopPoolSize() {
        VertxOptions opts = initializeAndCapture(
                VertxCoreProducerTest.vertxConfiguration().withOptional(VertxConfiguration::eventLoopsPoolSize, 4).build());
        Assertions.assertEquals(4, opts.getEventLoopPoolSize());
    }

    @Test
    public void shouldConfigureWorkerPoolSizeFromThreadPoolConfig() {
        VertxOptions opts = initializeAndCapture(
                VertxCoreProducerTest.vertxConfiguration().build(),
                VertxCoreProducerTest.threadPoolConfig().withOptional(ThreadPoolConfig::maxThreads, 42).build());
        Assertions.assertEquals(42, opts.getWorkerPoolSize());
    }

    @Test
    public void shouldConfigureMaxEventLoopExecuteTime() {
        VertxOptions opts = initializeAndCapture(VertxCoreProducerTest.vertxConfiguration()
                .with(VertxConfiguration::maxEventLoopExecuteTime, Duration.ofSeconds(5)).build());
        Assertions.assertEquals(Duration.ofSeconds(5).toMillis(), opts.getMaxEventLoopExecuteTime());
    }

    @Test
    public void shouldConfigureMaxWorkerExecuteTime() {
        VertxOptions opts = initializeAndCapture(VertxCoreProducerTest.vertxConfiguration()
                .with(VertxConfiguration::maxWorkerExecuteTime, Duration.ofSeconds(120)).build());
        Assertions.assertEquals(Duration.ofSeconds(120).toMillis(), opts.getMaxWorkerExecuteTime());
    }

    @Test
    public void shouldConfigureWarningExceptionTime() {
        VertxOptions opts = initializeAndCapture(VertxCoreProducerTest.vertxConfiguration()
                .with(VertxConfiguration::warningExceptionTime, Duration.ofSeconds(10)).build());
        Assertions.assertEquals(Duration.ofSeconds(10).toNanos(), opts.getWarningExceptionTime());
    }

    @Test
    public void shouldConfigurePreferNativeTransport() {
        VertxCoreRecorder.buildTimeConfig = ConfigInstanceBuilder.forInterface(VertxBuildTimeConfig.class)
                .with(VertxBuildTimeConfig::nativeTransport, NativeTransportMode.IF_AVAILABLE)
                .with(VertxBuildTimeConfig::nativeTransportType, NativeTransportType.AUTO)
                .build();
        try {
            VertxOptions opts = initializeAndCapture(VertxCoreProducerTest.vertxConfiguration().build());
            Assertions.assertTrue(opts.getPreferNativeTransport());
        } finally {
            VertxCoreRecorder.buildTimeConfig = null;
        }
    }

    @Test
    public void shouldDisableCaching() {
        VertxOptions opts = initializeAndCapture(
                VertxCoreProducerTest.vertxConfiguration().with(VertxConfiguration::caching, false).build());
        Assertions.assertNotNull(opts.getFileSystemOptions());
        Assertions.assertFalse(opts.getFileSystemOptions().isFileCachingEnabled());
    }

    @Test
    public void shouldDisableClasspathResolving() {
        VertxOptions opts = initializeAndCapture(
                VertxCoreProducerTest.vertxConfiguration().with(VertxConfiguration::classpathResolving, false).build());
        Assertions.assertNotNull(opts.getFileSystemOptions());
        Assertions.assertFalse(opts.getFileSystemOptions().isClassPathResolvingEnabled());
    }

    @Test
    public void defaultEventLoopPoolSizeIsNotExplicitlySet() {
        VertxOptions opts = initializeAndCapture(VertxCoreProducerTest.vertxConfiguration().build());
        Assertions.assertTrue(opts.getEventLoopPoolSize() > 0);
    }

    @Test
    public void shouldCallBootstrapCustomizer() {
        AtomicBoolean bootstrapCustomized = new AtomicBoolean(false);
        Consumer<VertxBootstrap> bootstrapConsumer = bootstrap -> {
            Assertions.assertNotNull(bootstrap);
            bootstrapCustomized.set(true);
        };

        AtomicReference<VertxOptions> captured = new AtomicReference<>();
        VertxCustomizer customizer = new VertxCustomizer(
                List.of(bootstrapConsumer), List.of(captured::set));
        VertxCoreRecorder.initialize(VertxCoreProducerTest.vertxConfiguration().build(),
                customizer, VertxCoreProducerTest.threadPoolConfig().build(), null,
                LaunchMode.TEST, List.of(), List.of());

        Assertions.assertTrue(bootstrapCustomized.get());
        Assertions.assertNotNull(captured.get());
    }
}
