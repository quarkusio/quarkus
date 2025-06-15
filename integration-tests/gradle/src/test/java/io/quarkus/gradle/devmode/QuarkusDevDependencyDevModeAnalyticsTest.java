package io.quarkus.gradle.devmode;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.gradle.BuildResult;

@Disabled("Manual testing only - You must have accepted analytics before running this test")
public class QuarkusDevDependencyDevModeAnalyticsTest extends QuarkusDevGradleTestBase {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final String TRACK_ENDPOINT = "v1/track";
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);
    private static final String IDENTITY_ENDPOINT = "v1/identify";

    @BeforeAll
    static void start() {
        wireMockServer.start();
        wireMockServer.stubFor(post(urlEqualTo("/" + IDENTITY_ENDPOINT)).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody("{\"status\":\"ok\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/" + TRACK_ENDPOINT)).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody("{\"status\":\"ok\"}")));
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Override
    protected String projectDirectoryName() {
        return "quarkus-dev-dependency-analytics";
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("Quarkus");
        wireMockServer
                .verify(postRequestedFor(urlEqualTo("/" + TRACK_ENDPOINT)).withRequestBody(notMatching("\\A\\s*\\z")) // Match
                        // non-empty
                        // body
                        .withRequestBody(matchingJsonPath("$.userId"))); // Match request with the specified field
    }

    @Override
    protected BuildResult build() throws Exception {
        String[] args = { "clean", "quarkusDev", "-Dquarkus.analytics.enabled=true",
                "-Dquarkus.analytics.uri.base=http://localhost:9300/", "-Dorg.gradle.debug=true", "--no-daemon",
                "-Dorg.gradle.debug.port=7000" };
        return runGradleWrapper(false, projectDir, false, args);
    }
}
