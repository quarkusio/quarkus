package io.quarkus.kubernetes.service.binding.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class ServiceBindingTest {

    private final Path root = Paths.get("src/test/resources/k8s");

    @Test
    void testInvalid() throws IOException {
        Path path = Files.createTempDirectory("invalid-binding");
        assertThatIllegalArgumentException().isThrownBy(() -> new ServiceBinding(path));
    }

    @Test
    void test() {
        ServiceBinding binding = new ServiceBinding(root.resolve("test-name"));
        assertThat(binding.getType()).isEqualTo("test-type-2");
        assertThat(binding.getProvider()).isEqualTo("test-provider-2");
        assertThat(binding.getProperties()).containsOnly(
                entry("test-secret-key", "test-secret-value-2"),
                entry("test-other-secret-key", "test-other-secret-value-2"));
    }

    @Test
    void testK8s() {
        //When bindings are provided as a k8s configmap secret pairs data files will be symlinks to hidden directories
        ServiceBinding binding = new ServiceBinding(root.resolve("test-k8s"));
        assertThat(binding.getType()).isEqualTo("test-type-1");
        assertThat(binding.getProvider()).isEqualTo("test-provider-1");
        assertThat(binding.getProperties()).containsExactly(entry("test-secret-key", "test-secret-value"));
    }

}
