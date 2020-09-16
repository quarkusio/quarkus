package io.quarkus.registry.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.registry.model.Registry;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class URLRegistryBuilderTest {

    @Test
    void shouldFailOnEmptyURLS() {
        assertThrows(IllegalStateException.class, new URLRegistryBuilder()::build);
    }

    @Test
    void shouldReadRegistry() throws IOException {
        URL url = getClass().getClassLoader().getResource("registry/registry.json");
        Registry registry = new URLRegistryBuilder().addURL(url).build();
        assertThat(registry.getCoreVersions()).isNotEmpty();
    }
}
