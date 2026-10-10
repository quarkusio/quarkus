package io.quarkus.observability.devresource.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.devservices.common.Labels;
import io.quarkus.runtime.LaunchMode;

class TestcontainerContainerTest {

    @Test
    void configuresStandardDevServicesLabels() {
        GenericContainer<?> delegate = new GenericContainer<>("test-image");
        TestcontainerContainer<?, ?> container = new TestcontainerContainer<>(delegate);

        container.configureDevServicesLabels(LaunchMode.DEVELOPMENT);

        assertThat(delegate.getLabels())
                .containsKey(Labels.QUARKUS_PROCESS_UUID)
                .containsEntry(Labels.QUARKUS_LAUNCH_MODE, LaunchMode.DEVELOPMENT.toString());
    }
}
