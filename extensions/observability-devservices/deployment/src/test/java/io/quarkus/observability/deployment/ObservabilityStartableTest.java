package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.ContainerConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.deployment.ObservabilityDevServiceProcessor.ObservabilityStartable;
import io.quarkus.observability.devresource.Container;
import io.quarkus.observability.devresource.DevResourceLifecycleManager;
import io.quarkus.runtime.LaunchMode;

class ObservabilityStartableTest {

    @Test
    void configuresLabelsBeforeStartingDevResource() {
        AtomicBoolean labelsConfigured = new AtomicBoolean();
        DevResourceLifecycleManager<ContainerConfig> dev = new TestDevResource(labelsConfigured);
        ObservabilityStartable startable = new ObservabilityStartable(dev, new TestContainerConfig(), null,
                Optional.empty(), LaunchMode.DEVELOPMENT);

        startable.start();

        assertThat(labelsConfigured).isTrue();
        assertThat(startable.getDevServiceConfig()).containsEntry("started", "true");
    }

    private static class TestDevResource implements DevResourceLifecycleManager<ContainerConfig> {
        private final AtomicBoolean labelsConfigured;

        private TestDevResource(AtomicBoolean labelsConfigured) {
            this.labelsConfigured = labelsConfigured;
        }

        @Override
        public ContainerConfig config(ModulesConfiguration configuration) {
            return new TestContainerConfig();
        }

        @Override
        public Container<ContainerConfig> container(ContainerConfig config, ModulesConfiguration root) {
            return new TestContainer(labelsConfigured);
        }

        @Override
        public Map<String, String> config(int privatePort, String host, int publicPort) {
            return Map.of();
        }

        @Override
        public Map<String, String> start() {
            assertThat(labelsConfigured).isTrue();
            return Map.of("started", "true");
        }

        @Override
        public void stop() {
        }
    }

    private static class TestContainer implements Container<ContainerConfig> {
        private final AtomicBoolean labelsConfigured;

        private TestContainer(AtomicBoolean labelsConfigured) {
            this.labelsConfigured = labelsConfigured;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public String getContainerId() {
            return "container-id";
        }

        @Override
        public void withStartupTimeout(Duration duration) {
        }

        @Override
        public void configureDevServicesLabels(LaunchMode launchMode) {
            labelsConfigured.set(true);
        }

        @Override
        public Closeable closeableCallback(String serviceName) {
            return () -> {
            };
        }
    }

    private static class TestContainerConfig extends AbstractContainerConfig {
        private TestContainerConfig() {
            super("test-image", false);
        }
    }
}
