package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.common.AbstractConfigSource;

/**
 * This test makes sure that the rest client configuration is loaded even if it's provided by a ConfigSource which doesn't
 * list its contents via {@link ConfigSource#getPropertyNames()} (e.g. {@link VaultLikeConfigSource}).
 *
 * This wasn't working when the configuration was accessed through a ConfigSource map - rest client initialization would fail
 * because no URI/URL configuration was obtained.
 */
public class VaultScenarioRestClientConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(EchoResource.class, EchoClient.class, VaultLikeConfigSource.class)
                    .addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                            "io.quarkus.restclient.configuration.VaultScenarioRestClientConfigTest$VaultLikeConfigSource"));

    @Inject
    @RestClient
    EchoClient echoClient;

    @Test
    public void testClient() {
        assertThat(echoClient.echo("Hello")).isEqualTo("Hello");
    }

    /**
     * Vaults do not allow to list their contents via {@link #getProperties()} or {@link #getPropertyNames()}.
     */
    public static class VaultLikeConfigSource extends AbstractConfigSource {

        public VaultLikeConfigSource() {
            super("Test config source", Integer.MAX_VALUE);
        }

        @Override
        public Map<String, String> getProperties() {
            return Collections.emptyMap();
        }

        @Override
        public Set<String> getPropertyNames() {
            return Collections.emptySet();
        }

        @Override
        public String getValue(String propertyName) {
            if ("quarkus.rest-client.\"io.quarkus.restclient.configuration.EchoClient\".url".equals(propertyName)) {
                return "http://localhost:${quarkus.http.test-port:8081}";
            }
            return null;
        }
    }
}
