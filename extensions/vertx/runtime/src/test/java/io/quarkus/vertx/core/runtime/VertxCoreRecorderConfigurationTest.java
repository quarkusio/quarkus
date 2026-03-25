package io.quarkus.vertx.core.runtime;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder.VertxOptionsCustomizer;
import io.vertx.core.VertxOptions;

public class VertxCoreRecorderConfigurationTest {

    private VertxCoreRecorder recorder;

    @BeforeEach
    public void setUp() {
        recorder = new VertxCoreRecorder(new RuntimeValue<>(), new RuntimeValue<>(), new RuntimeValue<>());
    }

    @AfterEach
    public void tearDown() {
        recorder.destroy();
    }

    private VertxOptions initializeAndCapture(VertxCoreProducerTest.DefaultVertxConfiguration config,
            VertxCoreProducerTest.DefaultThreadPoolConfig threadPoolConfig) {
        AtomicReference<VertxOptions> captured = new AtomicReference<>();
        VertxOptionsCustomizer customizer = new VertxOptionsCustomizer(List.of(captured::set));
        VertxCoreRecorder.initialize(config, customizer, threadPoolConfig, null, LaunchMode.TEST,
                List.of(), List.of());
        return captured.get();
    }

    private VertxOptions initializeAndCapture(VertxCoreProducerTest.DefaultVertxConfiguration config) {
        return initializeAndCapture(config, new VertxCoreProducerTest.DefaultThreadPoolConfig());
    }

    @Test
    public void shouldConfigureEventLoopPoolSize() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public OptionalInt eventLoopsPoolSize() {
                return OptionalInt.of(4);
            }
        });
        Assertions.assertEquals(4, opts.getEventLoopPoolSize());
    }

    @Test
    public void shouldConfigureWorkerPoolSizeFromThreadPoolConfig() {
        VertxOptions opts = initializeAndCapture(
                new VertxCoreProducerTest.DefaultVertxConfiguration(),
                new VertxCoreProducerTest.DefaultThreadPoolConfig() {
                    @Override
                    public OptionalInt maxThreads() {
                        return OptionalInt.of(42);
                    }
                });
        Assertions.assertEquals(42, opts.getWorkerPoolSize());
    }

    @Test
    public void shouldConfigureMaxEventLoopExecuteTime() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public Duration maxEventLoopExecuteTime() {
                return Duration.ofSeconds(5);
            }
        });
        Assertions.assertEquals(Duration.ofSeconds(5).toMillis(), opts.getMaxEventLoopExecuteTime());
    }

    @Test
    public void shouldConfigureMaxWorkerExecuteTime() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public Duration maxWorkerExecuteTime() {
                return Duration.ofSeconds(120);
            }
        });
        Assertions.assertEquals(Duration.ofSeconds(120).toMillis(), opts.getMaxWorkerExecuteTime());
    }

    @Test
    public void shouldConfigureWarningExceptionTime() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public Duration warningExceptionTime() {
                return Duration.ofSeconds(10);
            }
        });
        Assertions.assertEquals(Duration.ofSeconds(10).toNanos(), opts.getWarningExceptionTime());
    }

    @Test
    public void shouldConfigurePreferNativeTransport() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public boolean preferNativeTransport() {
                return true;
            }
        });
        Assertions.assertTrue(opts.getPreferNativeTransport());
    }

    @Test
    public void shouldDisableCaching() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public boolean caching() {
                return false;
            }
        });
        Assertions.assertNotNull(opts.getFileSystemOptions());
        Assertions.assertFalse(opts.getFileSystemOptions().isFileCachingEnabled());
    }

    @Test
    public void shouldDisableClasspathResolving() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration() {
            @Override
            public boolean classpathResolving() {
                return false;
            }
        });
        Assertions.assertNotNull(opts.getFileSystemOptions());
        Assertions.assertFalse(opts.getFileSystemOptions().isClassPathResolvingEnabled());
    }

    @Test
    public void defaultEventLoopPoolSizeIsNotExplicitlySet() {
        VertxOptions opts = initializeAndCapture(new VertxCoreProducerTest.DefaultVertxConfiguration());
        Assertions.assertTrue(opts.getEventLoopPoolSize() > 0);
    }
}
