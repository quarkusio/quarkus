package io.quarkus.spring.cloud.config.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

class SpringCloudConfigClientGatewayTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);

    @BeforeAll
    static void start() {
        wireMockServer.start();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetStubs() {
        wireMockServer.resetAll();
    }

    @Test
    void testBasicExchange() throws Exception {
        final SpringCloudConfigClientConfig config = configForTesting(DiscoveryConfigSetup.NOT_PRESENT);
        final SpringCloudConfigClientGateway sut = new VertxSpringCloudConfigGateway(config);
        final String applicationName = "foo";
        final String profile = "dev";
        final String springCloudConfigUrl = String.format(
                "/%s/%s/%s", applicationName, profile, config.label().get());
        wireMockServer.stubFor(WireMock.get(springCloudConfigUrl).willReturn(WireMock
                .okJson(getJsonStringForApplicationAndProfile(applicationName, profile))));

        Response response = sut.exchange(applicationName, profile).await().indefinitely();

        assertThat(response).isNotNull().satisfies(r -> {
            assertThat(r.getName()).isEqualTo("foo");
            assertThat(r.getProfiles()).containsExactly("dev");
            assertThat(r.getPropertySources()).hasSize(4);
            assertThat(r.getPropertySources().get(0)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("bar", "spam"), entry("foo", "from foo development"),
                        entry("democonfigclient.message", "hello from dev profile"));
            });
            assertThat(r.getPropertySources().get(1)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("my.prop", "from application-dev.yml"));
            });
            assertThat(r.getPropertySources().get(2)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("foo", "from foo props"),
                        entry("democonfigclient.message", "hello spring io"));
            });
            assertThat(r.getPropertySources().get(3)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("foo", "baz"));
            });
        });
    }

    @Test
    void testBasicExchangeWithDiscoveryDisabled() throws Exception {
        final SpringCloudConfigClientConfig config = configForTesting(DiscoveryConfigSetup.PRESENT_AND_DISABLED);
        final SpringCloudConfigClientGateway sut = new VertxSpringCloudConfigGateway(config);
        final String applicationName = "foo";
        final String profile = "dev";
        final String springCloudConfigUrl = String.format(
                "/%s/%s/%s", applicationName, profile, config.label().get());
        wireMockServer.stubFor(WireMock.get(springCloudConfigUrl).willReturn(WireMock
                .okJson(getJsonStringForApplicationAndProfile(applicationName, profile))));

        Response response = sut.exchange(applicationName, profile).await().indefinitely();

        assertThat(response).isNotNull().satisfies(r -> {
            assertThat(r.getName()).isEqualTo("foo");
            assertThat(r.getProfiles()).containsExactly("dev");
            assertThat(r.getPropertySources()).hasSize(4);
            assertThat(r.getPropertySources().get(0)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("bar", "spam"), entry("foo", "from foo development"),
                        entry("democonfigclient.message", "hello from dev profile"));
            });
            assertThat(r.getPropertySources().get(1)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("my.prop", "from application-dev.yml"));
            });
            assertThat(r.getPropertySources().get(2)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("foo", "from foo props"),
                        entry("democonfigclient.message", "hello spring io"));
            });
            assertThat(r.getPropertySources().get(3)).satisfies(ps -> {
                assertThat(ps.getSource()).contains(entry("foo", "baz"));
            });
        });
    }

    @Test
    void testOidcClientCredentialsExchange() throws Exception {
        final SpringCloudConfigClientConfig config = configForTesting(DiscoveryConfigSetup.NOT_PRESENT);
        final SpringCloudConfigClientConfig.OidcConfig oidc = Mockito.mock(SpringCloudConfigClientConfig.OidcConfig.class);
        when(oidc.tokenUrl()).thenReturn("http://localhost:" + MOCK_SERVER_PORT + "/realms/test/protocol/openid-connect/token");
        when(oidc.clientId()).thenReturn("config-client");
        when(oidc.clientSecret()).thenReturn(Optional.of("secret"));
        when(oidc.grantType()).thenReturn("client_credentials");
        when(oidc.username()).thenReturn(Optional.empty());
        when(oidc.password()).thenReturn(Optional.empty());
        when(oidc.scope()).thenReturn(Optional.of("config"));
        when(config.oidc()).thenReturn(Optional.of(oidc));
        final SpringCloudConfigClientGateway sut = new VertxSpringCloudConfigGateway(config);
        final String applicationName = "foo";
        final String profile = "dev";
        final String springCloudConfigUrl = String.format(
                "/%s/%s/%s", applicationName, profile, config.label().get());
        wireMockServer.stubFor(WireMock.post("/realms/test/protocol/openid-connect/token")
                .withHeader("Content-Type", WireMock.containing("application/x-www-form-urlencoded"))
                .withHeader("Authorization", WireMock.equalTo("Basic Y29uZmlnLWNsaWVudDpzZWNyZXQ="))
                .withFormParam("grant_type", WireMock.equalTo("client_credentials"))
                .withFormParam("scope", WireMock.equalTo("config"))
                .willReturn(WireMock.okJson("{\"access_token\":\"the-token\",\"token_type\":\"Bearer\"}")));
        wireMockServer.stubFor(WireMock.get(springCloudConfigUrl)
                .withHeader("Authorization", WireMock.equalTo("Bearer the-token"))
                .willReturn(WireMock.okJson(getJsonStringForApplicationAndProfile(applicationName, profile))));

        Response response = sut.exchange(applicationName, profile).await().indefinitely();

        assertThat(response).isNotNull().satisfies(r -> {
            assertThat(r.getName()).isEqualTo("foo");
            assertThat(r.getProfiles()).containsExactly("dev");
            assertThat(r.getPropertySources()).hasSize(4);
        });
        wireMockServer.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/realms/test/protocol/openid-connect/token")));
    }

    private String getJsonStringForApplicationAndProfile(String applicationName, String profile) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(String.format("/%s-%s.json", applicationName, profile)),
                Charset.defaultCharset());
    }

    private static SpringCloudConfigClientConfig configForTesting(DiscoveryConfigSetup discoveryPresent) {
        SpringCloudConfigClientConfig config = Mockito.mock(SpringCloudConfigClientConfig.class);
        when(config.url()).thenReturn("http://localhost:" + MOCK_SERVER_PORT);
        when(config.label()).thenReturn(Optional.of("master"));
        when(config.connectionTimeout()).thenReturn(Duration.ZERO);
        when(config.readTimeout()).thenReturn(Duration.ZERO);
        when(config.username()).thenReturn(Optional.empty());
        when(config.password()).thenReturn(Optional.empty());
        when(config.trustStore()).thenReturn(Optional.empty());
        when(config.keyStore()).thenReturn(Optional.empty());
        when(config.trustCerts()).thenReturn(false);
        when(config.headers()).thenReturn(new HashMap<>());
        when(config.ordinal()).thenReturn(450);
        when(config.oidc()).thenReturn(Optional.empty());

        switch (discoveryPresent) {
            case NOT_PRESENT -> when(config.discovery()).thenReturn(Optional.empty());
            case PRESENT_AND_DISABLED ->
                when(Mockito.mock(SpringCloudConfigClientConfig.DiscoveryConfig.class).enabled()).thenReturn(false);
        }

        return config;
    }

    private enum DiscoveryConfigSetup {
        PRESENT_AND_DISABLED,
        NOT_PRESENT;
    }
}
