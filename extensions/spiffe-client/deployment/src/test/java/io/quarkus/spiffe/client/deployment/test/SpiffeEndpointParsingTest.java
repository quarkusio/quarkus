package io.quarkus.spiffe.client.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spiffe.client.runtime.internal.SpiffeClientConfig;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

class SpiffeEndpointParsingTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.spiffe-client.devservices.enabled", "false");

    @Test
    void validUnixEndpoint() {
        assertThat(configWith("unix:///run/spire/agent.sock").endpointSocket())
                .hasValue(URI.create("unix:///run/spire/agent.sock"));
    }

    @Test
    void validTcpEndpoint() {
        assertThat(configWith("tcp://127.0.0.1:8080").endpointSocket())
                .hasValue(URI.create("tcp://127.0.0.1:8080"));
    }

    @Test
    void validUnixShortForm() {
        assertThat(configWith("unix:/run/spire/agent.sock").endpointSocket())
                .hasValue(URI.create("unix:/run/spire/agent.sock"));
    }

    @Test
    void blankEndpointTreatedAsAbsent() {
        assertThat(configWith("  ").endpointSocket()).isEmpty();
    }

    @Test
    void noSchemeThrows() {
        assertThatThrownBy(() -> configWith("blah"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixWithAuthorityThrows() {
        assertThatThrownBy(() -> configWith("unix://authority/path"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixBlankPathThrows() {
        assertThatThrownBy(() -> configWith("unix:///"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixOpaqueThrows() {
        assertThatThrownBy(() -> configWith("unix:opaque"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixWithQueryThrows() {
        assertThatThrownBy(() -> configWith("unix:///foo?query=1"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixWithFragmentThrows() {
        assertThatThrownBy(() -> configWith("unix:///foo#fragment"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unixRootPathOnlyThrows() {
        assertThatThrownBy(() -> configWith("unix:/"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpWithHostnameThrows() {
        assertThatThrownBy(() -> configWith("tcp://hostname:8080"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpNoPortThrows() {
        assertThatThrownBy(() -> configWith("tcp://127.0.0.1"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpNoHostThrows() {
        assertThatThrownBy(() -> configWith("tcp:///path"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpWithPathThrows() {
        assertThatThrownBy(() -> configWith("tcp://127.0.0.1:8080/path"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpWithQueryThrows() {
        assertThatThrownBy(() -> configWith("tcp://127.0.0.1:8080?q=1"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpWithFragmentThrows() {
        assertThatThrownBy(() -> configWith("tcp://127.0.0.1:8080#frag"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpWithUserinfoThrows() {
        assertThatThrownBy(() -> configWith("tcp://user:pass@127.0.0.1:8080"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpOpaqueThrows() {
        assertThatThrownBy(() -> configWith("tcp:opaque"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void tcpEmptyAuthorityThrows() {
        assertThatThrownBy(() -> configWith("tcp://"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void unknownSchemeThrows() {
        assertThatThrownBy(() -> configWith("http://127.0.0.1:8080"))
                .isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void fallbackToSpiffeEndpointSocketEnvVar() {
        SpiffeClientConfig config = configFromSources(new EnvConfigSource(
                Map.of("SPIFFE_ENDPOINT_SOCKET", "unix:///run/spire/sockets/agent.sock"), EnvConfigSource.ORDINAL));
        assertThat(config.endpointSocket())
                .hasValue(URI.create("unix:///run/spire/sockets/agent.sock"));
    }

    @Test
    void quarkusPropertyTakesPrecedenceOverEnvVar() {
        SpiffeClientConfig config = configFromSources(
                new PropertiesConfigSource(Map.of("quarkus.spiffe-client.endpoint-socket", "tcp://127.0.0.1:8080"), "test",
                        100),
                new EnvConfigSource(Map.of("SPIFFE_ENDPOINT_SOCKET", "unix:///run/spire/sockets/agent.sock"),
                        EnvConfigSource.ORDINAL));
        assertThat(config.endpointSocket())
                .hasValue(URI.create("tcp://127.0.0.1:8080"));
    }

    private static SpiffeClientConfig configWith(String endpointSocket) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(
                        Map.of("quarkus.spiffe-client.endpoint-socket", endpointSocket), "test", 100))
                .withMapping(SpiffeClientConfig.class)
                .build()
                .getConfigMapping(SpiffeClientConfig.class);
    }

    private static SpiffeClientConfig configFromSources(ConfigSource... configSources) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(configSources)
                .withMapping(SpiffeClientConfig.class)
                .build()
                .getConfigMapping(SpiffeClientConfig.class);
    }
}
