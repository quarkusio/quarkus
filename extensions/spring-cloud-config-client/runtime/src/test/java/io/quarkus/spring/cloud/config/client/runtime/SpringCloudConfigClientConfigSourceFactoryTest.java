package io.quarkus.spring.cloud.config.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.smallrye.config.ConfigSourceContext;

class SpringCloudConfigClientConfigSourceFactoryTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final int MOCK_EUREKA_SERVER_PORT = 8761;
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);
    private static final WireMockServer wireMockEurekaServer = new WireMockServer(MOCK_EUREKA_SERVER_PORT);

    @BeforeAll
    static void start() {

        wireMockServer.start();
        wireMockEurekaServer.start();
    }

    @AfterAll
    static void stop() {

        wireMockServer.stop();
        wireMockEurekaServer.stop();
    }

    @Test
    void testExtensionDisabled() {

        // Arrange
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(false, "foo", MOCK_SERVER_PORT, true, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Assert
        assertThat(configSourceIterable).isEmpty();
    }

    @Test
    void testNameNotProvided() {

        // Arrange
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(true, null, MOCK_SERVER_PORT, true, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Assert
        assertThat(configSourceIterable).isEmpty();
    }

    @Test
    void testInAppCDsGeneration() {

        // Arrange
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(true, "foo", MOCK_SERVER_PORT, true, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        System.setProperty(ApplicationLifecycleManager.QUARKUS_APPCDS_GENERATE_PROP, "true");

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Clear property, because not necessary any more
        System.clearProperty(ApplicationLifecycleManager.QUARKUS_APPCDS_GENERATE_PROP);

        // Assert
        assertThat(configSourceIterable).isEmpty();
    }

    @Test
    void testFailFastDisable() {

        // Arrange
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(true, "unknown-application", 1234, false, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        Mockito.when(context.getProfiles()).thenReturn(List.of("dev"));

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Assert
        assertThat(configSourceIterable).isEmpty();
    }

    @Test
    void testFailFastEnabled() {

        // Arrange
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(true, "unknown-application", 1234, true, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        Mockito.when(context.getProfiles()).thenReturn(List.of("dev"));

        // Act + Assert
        assertThatThrownBy(() -> factory.getConfigSources(context, config)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to obtain configuration from Spring Cloud Config Server at");
    }

    @Test
    void testBasic() throws IOException {

        // Arrange
        final String profile = "dev";
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);
        final SpringCloudConfigClientConfig config = configForTesting(true, "foo", MOCK_SERVER_PORT, true, 450, false);
        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        Mockito.when(context.getProfiles()).thenReturn(List.of(profile));
        wireMockServer.stubFor(WireMock.get(String.format("/%s/%s/%s", config.name(), profile, config.label().get()))
                .willReturn(WireMock.okJson(getJsonStringForApplicationAndProfile(config.name(), profile))));

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Assert
        assertThat(configSourceIterable).hasSize(4);
        assertThat(configSourceIterable).isInstanceOf(List.class);

        final List<ConfigSource> configSourceList = (List<ConfigSource>) configSourceIterable;
        assertThat(configSourceList.get(0)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/application.yml");
            assertThat(cs.getOrdinal()).isEqualTo(450);
            assertThat(cs.getProperties()).contains(entry("%dev.info.description", "Spring Cloud Samples"),
                    entry("%dev.foo", "baz"), entry("%dev.info.url", "https://github.com/spring-cloud-samples"),
                    entry("%dev.eureka.client.serviceUrl.defaultZone", "http://localhost:8761/eureka/"));
        });

        assertThat(configSourceList.get(1)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/foo.properties");
            assertThat(cs.getOrdinal()).isEqualTo(451);
            assertThat(cs.getProperties()).contains(entry("%dev.foo", "from foo props"),
                    entry("%dev.democonfigclient.message", "hello spring io"));
        });

        assertThat(configSourceList.get(2)).satisfies(cs -> {
            assertThat(cs.getName())
                    .isEqualTo("https://github.com/spring-cloud-samples/config-repo/application-dev.yml");
            assertThat(cs.getOrdinal()).isEqualTo(452);
            assertThat(cs.getProperties()).contains(entry("%dev.my.prop", "from application-dev.yml"));
        });

        assertThat(configSourceList.get(3)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/foo-dev.yml");
            assertThat(cs.getOrdinal()).isEqualTo(453);
            assertThat(cs.getProperties()).contains(entry("%dev.foo", "from foo development"),
                    entry("%dev.democonfigclient.message", "hello from dev profile"), entry("%dev.bar", "spam"));
        });
    }

    @Test
    void testDiscovery() throws IOException {

        // Arrange
        final String profile = "dev";
        final ConfigSourceContext context = Mockito.mock(ConfigSourceContext.class);

        final SpringCloudConfigClientConfig config = Mockito.mock(SpringCloudConfigClientConfig.class);
        when(config.enabled()).thenReturn(true);
        when(config.name()).thenReturn("foo");
        when(config.url()).thenReturn("http://localhost:" + MOCK_SERVER_PORT);
        when(config.label()).thenReturn(Optional.of("master"));
        when(config.failFast()).thenReturn(true);
        when(config.connectionTimeout()).thenReturn(Duration.ZERO);
        when(config.readTimeout()).thenReturn(Duration.ZERO);
        when(config.username()).thenReturn(Optional.empty());
        when(config.password()).thenReturn(Optional.empty());
        when(config.trustStore()).thenReturn(Optional.empty());
        when(config.keyStore()).thenReturn(Optional.empty());
        when(config.trustCerts()).thenReturn(false);
        when(config.headers()).thenReturn(new HashMap<>());
        when(config.ordinal()).thenReturn(450);
        SpringCloudConfigClientConfig.DiscoveryConfig discoveryConfig = Mockito
                .mock(SpringCloudConfigClientConfig.DiscoveryConfig.class);

        SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig eurekaConfig = Mockito
                .mock(SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig.class);

        when(config.discovery()).thenReturn(Optional.of(discoveryConfig));
        when(discoveryConfig.enabled()).thenReturn(true);
        when(discoveryConfig.serviceId()).thenReturn(Optional.of("config-server-id"));
        when(discoveryConfig.eurekaConfig()).thenReturn(Optional.of(eurekaConfig));
        when(eurekaConfig.serviceUrl())
                .thenReturn(Map.of("defaultZone", "http://localhost:" + MOCK_EUREKA_SERVER_PORT + "/eureka/"));
        when(eurekaConfig.registryFetchIntervalSeconds()).thenReturn(Duration.ofSeconds(30));

        final SpringCloudConfigClientConfigSourceFactory factory = new SpringCloudConfigClientConfigSourceFactory();

        Mockito.when(context.getProfiles()).thenReturn(List.of(profile));
        wireMockServer.stubFor(WireMock.get(String.format("/%s/%s/%s", config.name(), profile, config.label().get()))
                .willReturn(WireMock.okJson(getJsonStringForApplicationAndProfile(config.name(), profile))));

        wireMockEurekaServer.stubFor(WireMock.get("/eureka/apps/config-server-id").willReturn(
                WireMock.okJson(IOUtils.toString(
                        this.getClass().getResourceAsStream("/eureka-response.json"),
                        Charset.defaultCharset()))));

        // Act
        final Iterable<ConfigSource> configSourceIterable = factory.getConfigSources(context, config);

        // Assert
        assertThat(configSourceIterable).hasSize(4);
        assertThat(configSourceIterable).isInstanceOf(List.class);

        final List<ConfigSource> configSourceList = (List<ConfigSource>) configSourceIterable;
        assertThat(configSourceList.get(0)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/application.yml");
            assertThat(cs.getOrdinal()).isEqualTo(450);
            assertThat(cs.getProperties()).contains(entry("%dev.info.description", "Spring Cloud Samples"),
                    entry("%dev.foo", "baz"), entry("%dev.info.url", "https://github.com/spring-cloud-samples"));
        });

        assertThat(configSourceList.get(1)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/foo.properties");
            assertThat(cs.getOrdinal()).isEqualTo(451);
            assertThat(cs.getProperties()).contains(entry("%dev.foo", "from foo props"),
                    entry("%dev.democonfigclient.message", "hello spring io"));
        });

        assertThat(configSourceList.get(2)).satisfies(cs -> {
            assertThat(cs.getName())
                    .isEqualTo("https://github.com/spring-cloud-samples/config-repo/application-dev.yml");
            assertThat(cs.getOrdinal()).isEqualTo(452);
            assertThat(cs.getProperties()).contains(entry("%dev.my.prop", "from application-dev.yml"));
        });

        assertThat(configSourceList.get(3)).satisfies(cs -> {
            assertThat(cs.getName()).isEqualTo("https://github.com/spring-cloud-samples/config-repo/foo-dev.yml");
            assertThat(cs.getOrdinal()).isEqualTo(453);
            assertThat(cs.getProperties()).contains(entry("%dev.foo", "from foo development"),
                    entry("%dev.democonfigclient.message", "hello from dev profile"), entry("%dev.bar", "spam"));
        });
    }

    private SpringCloudConfigClientConfig configForTesting(final boolean isEnabled, final String appName,
            final int serverPort, final boolean isFailFastEnabled, final int ordinal, final boolean isDiscoveryEnabled) {

        final SpringCloudConfigClientConfig config = Mockito.mock(SpringCloudConfigClientConfig.class);
        when(config.enabled()).thenReturn(isEnabled);
        when(config.name()).thenReturn(appName);
        when(config.url()).thenReturn("http://localhost:" + serverPort);
        when(config.label()).thenReturn(Optional.of("master"));
        when(config.failFast()).thenReturn(isFailFastEnabled);
        when(config.connectionTimeout()).thenReturn(Duration.ZERO);
        when(config.readTimeout()).thenReturn(Duration.ZERO);
        when(config.username()).thenReturn(Optional.empty());
        when(config.password()).thenReturn(Optional.empty());
        when(config.trustStore()).thenReturn(Optional.empty());
        when(config.keyStore()).thenReturn(Optional.empty());
        when(config.trustCerts()).thenReturn(false);
        when(config.headers()).thenReturn(new HashMap<>());
        when(config.ordinal()).thenReturn(ordinal);
        SpringCloudConfigClientConfig.DiscoveryConfig discoveryConfig = Mockito
                .mock(SpringCloudConfigClientConfig.DiscoveryConfig.class);

        SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig eurekaConfig = Mockito
                .mock(SpringCloudConfigClientConfig.DiscoveryConfig.EurekaConfig.class);

        when(config.discovery()).thenReturn(Optional.of(discoveryConfig));
        when(discoveryConfig.enabled()).thenReturn(isDiscoveryEnabled);
        when(discoveryConfig.serviceId()).thenReturn(Optional.of("config-server-id"));
        when(discoveryConfig.eurekaConfig()).thenReturn(Optional.of(eurekaConfig));
        when(eurekaConfig.serviceUrl()).thenReturn(Map.of("defaultZone", "http://localhost:8761/eureka/"));

        return config;
    }

    private String getJsonStringForApplicationAndProfile(final String applicationName, final String profile)
            throws IOException {

        return IOUtils.toString(
                this.getClass().getResourceAsStream(String.format("/%s-%s.json", applicationName, profile)),
                Charset.defaultCharset());
    }
}
