package io.quarkus.restclient.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.common.MapBackedConfigSource;

public class RestClientBuildTimeConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    private static final Map<String, String> BUILD_TIME_PROPERTIES = Map.of(
            "io.quarkus.restclient.configuration.EchoClient/mp-rest/url", "http://nohost",
            "BT-MP/mp-rest/url", "from-mp",
            "BT-QUARKUS-MP/mp-rest/url", "from-mp",
            "quarkus.rest-client.BT-QUARKUS-MP.url", "from-quarkus");

    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        boolean isBuildTime = false;
        for (ConfigSource source : builder.getSources()) {
            if ("PropertiesConfigSource[source=Build system]".equals(source.getName())) {
                isBuildTime = true;
                break;
            }
        }

        if (isBuildTime) {
            // A build time only source to test the recording of configuration values.
            builder.withSources(
                    new MapBackedConfigSource("RestClientBuildTimeConfigSource", BUILD_TIME_PROPERTIES, Integer.MAX_VALUE) {
                    });
        } else {
            builder.withSources(new MapBackedConfigSource("RestClientRuntimeConfigSource", Map.of(), Integer.MAX_VALUE) {
                @Override
                public String getValue(final String propertyName) {
                    if (!propertyName.equals("io.quarkus.restclient.configuration.EchoClient/mp-rest/url")) {
                        return null;
                    }

                    return "http://localhost:${quarkus.http.test-port:8081}";
                }

                @Override
                public Set<String> getPropertyNames() {
                    return Collections.singleton("io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
                }
            });
        }
    }
}
