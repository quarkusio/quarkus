package io.quarkus.restclient.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

public class RestClientOverrideRuntimeConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class, EchoClient.class, RestClientBuildTimeConfigSource.class,
                            RestClientRunTimeConfigSource.class)
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.restclient.configuration.RestClientBuildTimeConfigSource",
                            "io.quarkus.restclient.configuration.RestClientRunTimeConfigSource"));

    @Inject
    @RestClient
    EchoClient echoClient;
    @Inject
    SmallRyeConfig config;

    @Test
    void overrideConfig() {
        // Build time property recording
        Optional<ConfigSource> specifiedDefaultValues = config.getConfigSource("DefaultValuesConfigSource");
        assertTrue(specifiedDefaultValues.isPresent());
        assertTrue(specifiedDefaultValues.get().getPropertyNames()
                .contains("io.quarkus.restclient.configuration.EchoClient/mp-rest/url"));
        assertEquals("http://nohost",
                specifiedDefaultValues.get().getValue("io.quarkus.restclient.configuration.EchoClient/mp-rest/url"));
        // This config key comes from the interceptor. It is available in propertyNames, but is not recorded
        assertNull(specifiedDefaultValues.get()
                .getValue("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url"));
        assertTrue(StreamSupport.stream(config.getPropertyNames().spliterator(), false).anyMatch(
                property -> property.equals("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url")));

        // Override MP Build time property with Quarkus property
        ConfigValue mpValue = config.getConfigValue("io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
        // Fallbacks from runtime to the override build time value
        ConfigValue quarkusValue = config
                .getConfigValue("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url");
        assertEquals(mpValue.getValue(), quarkusValue.getValue());
        assertEquals(RestClientRunTimeConfigSource.class.getName(), quarkusValue.getConfigSourceName());
        // The MP name has priority over the Quarkus one, so it is the name we get (even when we look up the quarkus one)
        assertEquals(mpValue.getName(), "io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
        assertEquals(quarkusValue.getName(), "io.quarkus.restclient.configuration.EchoClient/mp-rest/url");

        assertEquals("Hi", echoClient.echo("Hi"));
    }
}
