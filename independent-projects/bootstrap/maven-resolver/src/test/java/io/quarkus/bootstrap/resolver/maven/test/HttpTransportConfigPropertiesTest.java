package io.quarkus.bootstrap.resolver.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;

/**
 * Maven Resolver 2.x renamed the HTTP transport configuration keys from {@code aether.connector.*}
 * to {@code aether.transport.http.*} and replaced {@code HttpTransporterFactory} with {@code ApacheTransporterFactory}.
 * The bootstrap session must be understood by both resolver 1.9 (Maven 3.9) and resolver 2.x (Maven 3.10),
 * whichever is on the classpath at runtime, independently of the version Quarkus was compiled against.
 */
public class HttpTransportConfigPropertiesTest extends BootstrapMavenContextTestBase {

    @Test
    public void userAgentIsSetForResolver1And2() throws Exception {
        final Map<String, Object> props = configProperties();
        assertThat(props.get("aether.transport.http.userAgent")).isNotNull();
        assertThat(props.get("aether.connector.userAgent")).isEqualTo(props.get("aether.transport.http.userAgent"));
    }

    @Test
    public void serverHttpConfigurationIsSetForResolver1And2() throws Exception {
        final Map<String, Object> props = configProperties();

        assertThat(props)
                .containsEntry("aether.transport.http.connectTimeout.custom-repo", 1234)
                .containsEntry("aether.connector.connectTimeout.custom-repo", 1234)
                .containsEntry("aether.transport.http.requestTimeout.custom-repo", 5678)
                .containsEntry("aether.connector.requestTimeout.custom-repo", 5678)
                .containsEntry("aether.transport.http.headers.custom-repo", Map.of("X-Custom-Header", "custom-value"))
                .containsEntry("aether.connector.http.headers.custom-repo", Map.of("X-Custom-Header", "custom-value"));
    }

    @Test
    public void nativeTransportIsPrioritizedForResolver1And2() throws Exception {
        setSystemProp("maven.resolver.transport", "native");
        final Map<String, Object> props = configProperties();

        final String maxPriority = String.valueOf(Float.MAX_VALUE);
        assertThat(props)
                .containsEntry("aether.priority.FileTransporterFactory", maxPriority)
                .containsEntry("aether.priority.HttpTransporterFactory", maxPriority)
                .containsEntry("aether.priority.ApacheTransporterFactory", maxPriority)
                .doesNotContainKey("aether.priority.WagonTransporterFactory");
    }

    @Test
    public void wagonTransportIsPrioritized() throws Exception {
        setSystemProp("maven.resolver.transport", "wagon");
        final Map<String, Object> props = configProperties();

        assertThat(props)
                .containsEntry("aether.priority.WagonTransporterFactory", String.valueOf(Float.MAX_VALUE))
                .doesNotContainKeys("aether.priority.HttpTransporterFactory", "aether.priority.ApacheTransporterFactory");
    }

    private Map<String, Object> configProperties() throws Exception {
        final BootstrapMavenContext mvn = bootstrapMavenContextWithSettings("custom-settings/http-server-config");
        return mvn.getRepositorySystemSession().getConfigProperties();
    }
}
