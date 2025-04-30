package io.quarkus.analytics.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.quarkus.analytics.common.ContextTestData.createContext;
import static io.quarkus.analytics.rest.RestClient.IDENTITY_ENDPOINT;
import static io.quarkus.analytics.util.StringUtils.getObjectMapper;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;
import io.quarkus.analytics.dto.config.Identity;

class RestClientFailTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final String TEST_CONFIG_URL = "http://localhost:" + MOCK_SERVER_PORT + "/" + "config";
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);

    @BeforeAll
    static void start() throws JsonProcessingException {
        System.setProperty("quarkus.analytics.timeout", "200");
        wireMockServer.start();
        wireMockServer.stubFor(post(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withFixedDelay(5000) // must be bigger than timeout
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
        wireMockServer.stubFor(get(urlEqualTo("/config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000) // must be bigger than timeout
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"active\":true,\"deny_user_ids\":[],\"deny_quarkus_versions\":[],\"refresh_interval\":43200.000000000}")));

    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
        System.clearProperty("quarkus.analytics.timeout");
    }

    @Test
    void postIdentityServerTTLExceeded() throws URISyntaxException, JsonProcessingException {
        RestClient restClient = new RestClient();
        Identity identity = createIdentity();
        CompletableFuture<HttpResponse<String>> post = restClient.post(
                identity,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + IDENTITY_ENDPOINT));

        try {
            post.get(100, TimeUnit.MILLISECONDS).statusCode();
            fail("Should have thrown an TimeoutException or ExecutionException");
        } catch (ExecutionException | TimeoutException e) {
            // ok
        } catch (Exception e) {
            fail("Should have thrown an TimeoutException or ExecutionException");
        }

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .withRequestBody(equalToJson(getObjectMapper().writeValueAsString(identity))));
    }

    @Test
    void getConfigServerTTLExceeded() throws URISyntaxException {
        RestClient restClient = new RestClient();
        Optional<AnalyticsRemoteConfig> analyticsConfig = restClient.getConfig(new URI(TEST_CONFIG_URL));
        assertNotNull(analyticsConfig);
        assertEquals(Optional.empty(), analyticsConfig);
    }

    private Identity createIdentity() {
        return Identity.builder()
                .context(createContext())
                .userId("12345678901234567890")
                .timestamp(Instant.now()).build();
    }
}
