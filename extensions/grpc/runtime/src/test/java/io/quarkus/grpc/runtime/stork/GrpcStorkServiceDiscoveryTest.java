package io.quarkus.grpc.runtime.stork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.NameResolver;
import io.grpc.NameResolver.ConfigOrError;
import io.grpc.Status;
import io.grpc.SynchronizationContext;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.integration.DefaultStorkInfrastructure;

class GrpcStorkServiceDiscoveryTest {

    private static final String SERVICE_NAME = "hello-service";

    private ScheduledExecutorService scheduledExecutorService;

    @BeforeEach
    void setUp() {
        Stork.shutdown();
        Stork.initialize(new DefaultStorkInfrastructure());
    }

    @AfterEach
    void tearDown() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
        Stork.shutdown();
    }

    @Test
    void refreshNotifiesListenerWhenInstancesUnchanged() {
        defineService(staticDiscovery());
        CountingListener listener = new CountingListener();
        NameResolver resolver = createResolver();

        resolver.start(listener);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(listener.results.get()).isEqualTo(1));

        resolver.refresh();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(listener.results.get()).isEqualTo(2));
        assertThat(listener.errors.get()).isZero();
    }

    @Test
    void refreshAfterDiscoveryFailureNotifiesListener() {
        defineService(flakyDiscovery());
        CountingListener listener = new CountingListener();
        NameResolver resolver = createResolver();

        resolver.start(listener);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(listener.errors.get()).isEqualTo(1));
        assertThat(listener.results.get()).isZero();
        assertThat(listener.lastError.get().getCode()).isEqualTo(Status.Code.UNAVAILABLE);

        // If resolving stayed true after the async failure, refresh() would no-op.
        resolver.refresh();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(listener.results.get()).isEqualTo(1));
        assertThat(listener.errors.get()).isEqualTo(1);
    }

    @Test
    void doesNotNotifyListenerAfterShutdownOnDiscoveryFailure() {
        defineService(flakyDiscovery(Map.of(
                "address-list", "localhost:9001",
                "failure-delay-ms", "500")));
        CountingListener listener = new CountingListener();
        NameResolver resolver = createResolver();

        resolver.start(listener);
        resolver.shutdown();

        // The delayed failure must not notify the listener once shutdown has completed.
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(listener.errors.get()).isZero();
            assertThat(listener.results.get()).isZero();
        });
    }

    @Test
    void doesNotNotifyListenerAfterShutdownOnEmptyResolution() {
        defineService(flakyDiscovery(Map.of(
                "address-list", "localhost:9001",
                "empty-on-first-lookup", "true",
                "failure-delay-ms", "500")));
        CountingListener listener = new CountingListener();
        NameResolver resolver = createResolver();

        resolver.start(listener);
        resolver.shutdown();

        // The delayed empty resolution must not notify the listener once shutdown has completed.
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(listener.errors.get()).isZero();
            assertThat(listener.results.get()).isZero();
        });
    }

    private void defineService(ConfigWithType discovery) {
        Stork.getInstance().defineIfAbsent(SERVICE_NAME, ServiceDefinition.of(discovery));
    }

    private NameResolver createResolver() {
        GrpcStorkServiceDiscovery provider = new GrpcStorkServiceDiscovery();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        NameResolver.Args args = NameResolver.Args.newBuilder()
                .setDefaultPort(9001)
                .setProxyDetector(address -> null)
                .setSynchronizationContext(new SynchronizationContext((t, e) -> {
                    throw new RuntimeException(e);
                }))
                .setScheduledExecutorService(scheduledExecutorService)
                .setServiceConfigParser(new NameResolver.ServiceConfigParser() {
                    @Override
                    public ConfigOrError parseServiceConfig(Map<String, ?> rawServiceConfig) {
                        return ConfigOrError.fromConfig(Map.of());
                    }
                })
                .build();
        NameResolver resolver = provider.newNameResolver(URI.create("stork://" + SERVICE_NAME + ":9001"), args);
        assertThat(resolver).isNotNull();
        return resolver;
    }

    private static ConfigWithType staticDiscovery() {
        return new ConfigWithType() {
            @Override
            public String type() {
                return "static";
            }

            @Override
            public Map<String, String> parameters() {
                return Map.of("address-list", "localhost:9001");
            }
        };
    }

    private static ConfigWithType flakyDiscovery() {
        return flakyDiscovery(Map.of("address-list", "localhost:9001"));
    }

    private static ConfigWithType flakyDiscovery(Map<String, String> parameters) {
        return new ConfigWithType() {
            @Override
            public String type() {
                return "flaky";
            }

            @Override
            public Map<String, String> parameters() {
                return parameters;
            }
        };
    }

    private static final class CountingListener extends NameResolver.Listener2 {
        private final AtomicInteger results = new AtomicInteger();
        private final AtomicInteger errors = new AtomicInteger();
        private final AtomicReference<Status> lastError = new AtomicReference<>();

        @Override
        public void onResult(NameResolver.ResolutionResult resolutionResult) {
            results.incrementAndGet();
        }

        @Override
        public void onError(Status status) {
            errors.incrementAndGet();
            lastError.set(status);
        }
    }
}
