package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;

class DevServicesRegistryBuildItemTest {

    private final DevServicesRegistryBuildItem registry = new DevServicesRegistryBuildItem(UUID.randomUUID(), null,
            LaunchMode.TEST);

    @AfterEach
    void cleanup() {
        registry.closeOwnRunningServices();
    }

    @Test
    void shouldResolveDependencyChainsMoreThanOneLevelDeep() {
        DevServicesResultBuildItem serviceA = DevServicesResultBuildItem.owned()
                .feature("A")
                .startable(() -> new RecordingStartable("valueA", 0))
                .configProvider(Map.of("test.a.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesResultBuildItem serviceB = DevServicesResultBuildItem.owned()
                .feature("B")
                .startable(() -> new RecordingStartable("valueB", 300))
                .dependsOnConfig("test.a.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.b.value",
                        s -> s.getConnectionInfo() + "-" + s.getInjectedValue()))
                .build();

        DevServicesResultBuildItem serviceC = DevServicesResultBuildItem.owned()
                .feature("C")
                .startable(() -> new RecordingStartable("valueC", 0))
                .dependsOnConfig("test.b.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.c.value",
                        s -> s.getConnectionInfo() + "-" + s.getInjectedValue()))
                .build();

        // Services are passed in an order which does not match the dependency chain, on purpose
        DevServicesRegistryBuildItem.DevServicesStartResult result = registry.startAll(
                List.of(serviceC, serviceA, serviceB), List.of(), List.of(), null);

        assertEquals("valueA", result.configs().get("test.a.value"));
        assertEquals("valueB-valueA", result.configs().get("test.b.value"));
        assertEquals("valueC-valueB-valueA", result.configs().get("test.c.value"));
    }

    @Test
    void shouldNotStartAServiceWhoseTransitiveDependencyNeverBecomesAvailable() {
        DevServicesResultBuildItem serviceB = DevServicesResultBuildItem.owned()
                .feature("B")
                .startable(() -> new RecordingStartable("valueB", 0))
                .dependsOnConfig("test.never.available", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.b.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesResultBuildItem serviceC = DevServicesResultBuildItem.owned()
                .feature("C")
                .startable(() -> new RecordingStartable("valueC", 0))
                .dependsOnConfig("test.b.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.c.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesRegistryBuildItem.DevServicesStartResult result = registry.startAll(
                List.of(serviceB, serviceC), List.of(), List.of(), null);

        assertNull(result.configs().get("test.b.value"));
        assertNull(result.configs().get("test.c.value"));
    }

    @Test
    void shouldNotHangOnACircularDependency() {
        DevServicesResultBuildItem serviceX = DevServicesResultBuildItem.owned()
                .feature("X")
                .startable(() -> new RecordingStartable("valueX", 0))
                .dependsOnConfig("test.y.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.x.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesResultBuildItem serviceY = DevServicesResultBuildItem.owned()
                .feature("Y")
                .startable(() -> new RecordingStartable("valueY", 0))
                .dependsOnConfig("test.x.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.y.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesRegistryBuildItem.DevServicesStartResult result = registry.startAll(
                List.of(serviceX, serviceY), List.of(), List.of(), null);

        assertNull(result.configs().get("test.x.value"));
        assertNull(result.configs().get("test.y.value"));
    }

    @Test
    void shouldNotBlockChainOnAnUnsatisfiedOptionalDependency() {
        DevServicesResultBuildItem serviceB = DevServicesResultBuildItem.owned()
                .feature("B")
                .startable(() -> new RecordingStartable("valueB", 0))
                .dependsOnConfig("test.never.available", RecordingStartable::setInjectedValue, true)
                .configProvider(Map.of("test.b.value", RecordingStartable::getConnectionInfo))
                .build();

        DevServicesResultBuildItem serviceC = DevServicesResultBuildItem.owned()
                .feature("C")
                .startable(() -> new RecordingStartable("valueC", 0))
                .dependsOnConfig("test.b.value", RecordingStartable::setInjectedValue)
                .configProvider(Map.of("test.c.value",
                        s -> s.getConnectionInfo() + "-" + s.getInjectedValue()))
                .build();

        DevServicesRegistryBuildItem.DevServicesStartResult result = registry.startAll(
                List.of(serviceB, serviceC), List.of(), List.of(), null);

        assertEquals("valueB", result.configs().get("test.b.value"));
        assertEquals("valueC-valueB", result.configs().get("test.c.value"));
    }

    public static class RecordingStartable implements Startable {

        private final String connectionInfo;
        private final long startDelayMillis;
        private volatile String injectedValue;

        public RecordingStartable(String connectionInfo, long startDelayMillis) {
            this.connectionInfo = connectionInfo;
            this.startDelayMillis = startDelayMillis;
        }

        public void setInjectedValue(String injectedValue) {
            this.injectedValue = injectedValue;
        }

        public String getInjectedValue() {
            return injectedValue;
        }

        @Override
        public void start() {
            if (startDelayMillis > 0) {
                try {
                    Thread.sleep(startDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public String getConnectionInfo() {
            return connectionInfo;
        }

        @Override
        public String getContainerId() {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
