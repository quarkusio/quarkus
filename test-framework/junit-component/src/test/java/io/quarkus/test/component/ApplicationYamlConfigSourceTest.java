package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ApplicationYamlConfigSourceTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .useDiscoveredConfigSources(true)
            .build();

    @Inject
    YamlComponent component;

    @Test
    public void testYamlConfig() {
        assertEquals("from-yaml", component.yamlProperty);
    }

    @Singleton
    public static class YamlComponent {

        @ConfigProperty(name = "org.acme.yaml-property")
        String yamlProperty;
    }
}
