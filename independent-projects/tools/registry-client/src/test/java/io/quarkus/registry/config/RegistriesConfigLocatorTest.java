package io.quarkus.registry.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

public class RegistriesConfigLocatorTest {

    @Test
    public void configCompletion() throws Exception {

        final JsonRegistriesConfig config = new JsonRegistriesConfig();
        final JsonRegistryConfig registry = new JsonRegistryConfig("registry.acme.org");
        config.addRegistry(registry);
        registry.setUpdatePolicy("always");

        final RegistriesConfig completeConfig = serializeDeserialize(config);
        assertThat(completeConfig.getRegistries().size()).isEqualTo(1);
        final RegistryConfig completeRegistry = completeConfig.getRegistries().get(0);
        assertThat(completeRegistry.getId()).isEqualTo("registry.acme.org");
        assertThat(completeRegistry.getUpdatePolicy()).isEqualTo("always");
        final RegistryMavenConfig mavenConfig = completeRegistry.getMaven();
        assertThat(mavenConfig).isNull();
    }

    private static RegistriesConfig serializeDeserialize(RegistriesConfig config) throws Exception {
        final StringWriter buf = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(config, buf);
        try (StringReader reader = new StringReader(buf.toString())) {
            return RegistriesConfigLocator.load(reader);
        }
    }
}
