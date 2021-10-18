package io.quarkus.restclient.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import javax.inject.Inject;

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
        Optional<ConfigSource> specifiedDefaultValues = config
                .getConfigSource("PropertiesConfigSource[source=Specified default values]");
        assertTrue(specifiedDefaultValues.isPresent());
        assertTrue(specifiedDefaultValues.get().getPropertyNames()
                .contains("io.quarkus.restclient.configuration.EchoClient/mp-rest/url"));
        // This config key comes from the interceptor and it is recorded with an empty value. This allow the user to still override using the original key
        assertEquals("", specifiedDefaultValues.get()
                .getValue("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url"));

        ConfigValue mpValue = config.getConfigValue("io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
        // Fallbacks from runtime to the override build time value
        ConfigValue quarkusValue = config
                .getConfigValue("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url");
        assertEquals(mpValue.getValue(), quarkusValue.getValue());
        assertEquals(RestClientRunTimeConfigSource.class.getName(), quarkusValue.getConfigSourceName());

        assertEquals("Hi", echoClient.echo("Hi"));
    }
}
