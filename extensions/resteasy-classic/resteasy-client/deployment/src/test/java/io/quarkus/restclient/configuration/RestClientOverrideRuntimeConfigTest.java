package io.quarkus.restclient.configuration;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

public class RestClientOverrideRuntimeConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class, EchoClient.class, RestClientBuildTimeConfigBuilderCustomizer.class)
                    .addAsServiceProvider("io.smallrye.config.SmallRyeConfigBuilderCustomizer",
                            "io.quarkus.restclient.configuration.RestClientBuildTimeConfigBuilderCustomizer"));

    @Inject
    @RestClient
    EchoClient echoClient;
    @Inject
    SmallRyeConfig config;
    @Inject
    RestClientsConfig restClientsConfig;

    @Test
    void overrideConfig() {
        // Build time property recording
        Optional<ConfigSource> specifiedDefaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(specifiedDefaultValues.isPresent());
        assertTrue(specifiedDefaultValues.get().getPropertyNames()
                .contains("io.quarkus.restclient.configuration.EchoClient/mp-rest/url"));
        assertEquals("http://nohost",
                specifiedDefaultValues.get().getValue("io.quarkus.restclient.configuration.EchoClient/mp-rest/url"));
        assertTrue(StreamSupport.stream(config.getPropertyNames().spliterator(), false).anyMatch(
                property -> property.equals("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url")));

        // Override MP Build time property with Quarkus property
        ConfigValue mpValue = config.getConfigValue("io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
        // Fallbacks from runtime to the override build time value
        ConfigValue quarkusValue = config
                .getConfigValue("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url");
        assertEquals(mpValue.getValue(), quarkusValue.getValue());
        assertEquals("RestClientRuntimeConfigSource", quarkusValue.getConfigSourceName());
        // There is no relocate for MP names, so it keeps the same name
        assertEquals(mpValue.getName(), "io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
        // We use the Quarkus name, because that is the one that has priority
        assertEquals(quarkusValue.getName(), "quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url");

        assertTrue(restClientsConfig.clients().containsKey("io.quarkus.restclient.configuration.EchoClient"));
        Optional<String> url = restClientsConfig.clients().get("io.quarkus.restclient.configuration.EchoClient").url();
        assertTrue(url.isPresent());
        assertEquals(url.get(), mpValue.getValue());
        assertEquals(url.get(), quarkusValue.getValue());

        // overrides nohost -> localhost so the invoke succeeds
        assertEquals("Hi", echoClient.echo("Hi"));
    }

    @Test
    void buildTime() {
        Set<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        // MP/mp-rest/url - This one exists at build time
        assertTrue(properties.contains("BT-MP/mp-rest/url"));
        assertEquals("from-mp", config.getRawValue("BT-MP/mp-rest/url"));
        // quarkus.rest-client.MP.url - Is not set, and it is not recorded
        assertFalse(properties.contains("quarkus.rest-client.BT-MP.url"));

        // Both properties exist
        assertTrue(properties.contains("BT-QUARKUS-MP/mp-rest/url"));
        assertTrue(properties.contains("quarkus.rest-client.BT-QUARKUS-MP.url"));

        // There is no relocate for the MP property (only fallback), so each will get their own value
        ConfigValue mpValue = config.getConfigValue("BT-QUARKUS-MP/mp-rest/url");
        assertEquals("BT-QUARKUS-MP/mp-rest/url", mpValue.getName());
        assertEquals("from-mp", mpValue.getValue());
        ConfigValue quarkusValue = config.getConfigValue("quarkus.rest-client.BT-QUARKUS-MP.url");
        assertEquals("quarkus.rest-client.BT-QUARKUS-MP.url", quarkusValue.getName());
        assertEquals("from-quarkus", quarkusValue.getValue());
    }
}
