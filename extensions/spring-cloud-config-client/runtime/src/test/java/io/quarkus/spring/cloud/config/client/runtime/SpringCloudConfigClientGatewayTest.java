package io.quarkus.spring.cloud.config.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.runtime.TlsConfig;

class SpringCloudConfigClientGatewayTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);

    private static final SpringCloudConfigClientConfig springCloudConfigClientConfig = configForTesting();
    private final SpringCloudConfigClientGateway sut = new VertxSpringCloudConfigGateway(
            springCloudConfigClientConfig, new TlsConfig());

    @BeforeAll
    static void start() {
        wireMockServer.start();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Test
    void testBasicExchange() throws Exception {
        final String applicationName = "foo";
        final String profile = "dev";
        final String springCloudConfigUrl = String.format(
                "/%s/%s/%s", applicationName, profile, springCloudConfigClientConfig.label.get());
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

    private String getJsonStringForApplicationAndProfile(String applicationName, String profile) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(String.format("/%s-%s.json", applicationName, profile)),
                Charset.defaultCharset());
    }

    private static SpringCloudConfigClientConfig configForTesting() {
        SpringCloudConfigClientConfig springCloudConfigClientConfig = new SpringCloudConfigClientConfig();
        springCloudConfigClientConfig.url = "http://localhost:" + MOCK_SERVER_PORT;
        springCloudConfigClientConfig.label = Optional.of("master");
        springCloudConfigClientConfig.connectionTimeout = Duration.ZERO;
        springCloudConfigClientConfig.readTimeout = Duration.ZERO;
        springCloudConfigClientConfig.username = Optional.empty();
        springCloudConfigClientConfig.password = Optional.empty();
        springCloudConfigClientConfig.trustStore = Optional.empty();
        springCloudConfigClientConfig.keyStore = Optional.empty();
        springCloudConfigClientConfig.trustCerts = false;
        springCloudConfigClientConfig.headers = new HashMap<>();
        return springCloudConfigClientConfig;
    }
}
