package io.quarkus.analytics.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.quarkus.analytics.common.ContextTestData.createContext;
import static io.quarkus.analytics.rest.RestClient.IDENTITY_ENDPOINT;
import static io.quarkus.analytics.rest.RestClient.TRACK_ENDPOINT;
import static io.quarkus.analytics.util.StringUtils.getObjectMapper;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.dto.segment.Track;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.analytics.dto.segment.TrackProperties;

class RestClientTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final String TEST_CONFIG_URL = "http://localhost:" + MOCK_SERVER_PORT + "/" + "config";
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);

    @BeforeAll
    static void start() throws JsonProcessingException {
        wireMockServer.start();
        wireMockServer.stubFor(post(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/" + TRACK_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
        wireMockServer.stubFor(get(urlEqualTo("/config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        //                        .withBody(getObjectMapper().writeValueAsString(createRemoteConfig()))));
                        .withBody(
                                "{\"active\":true,\"deny_anonymous_ids\":[],\"deny_quarkus_versions\":[],\"refresh_interval\":43200.000000000}")));

    }

    private static RemoteConfig createRemoteConfig() {
        return RemoteConfig.builder()
                .active(true)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(12)).build();
    }

    @AfterAll
    static void stop() {
        wireMockServer.stop();
    }

    @Test
    void getUri() {
        assertNotNull(RestClient.CONFIG_URI);
    }

    @Test
    void getAuthHeaderFromDocs() {
        assertEquals("Basic YWJjMTIzOg==", RestClient.getAuthHeader("abc123"));
    }

    @Test
    void postIdentity()
            throws URISyntaxException, JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Identity identity = createIdentity();
        CompletableFuture<HttpResponse<String>> post = restClient.post(identity,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + IDENTITY_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .withRequestBody(equalToJson(getObjectMapper().writeValueAsString(identity))));
    }

    @Test
    void postTrace()
            throws URISyntaxException, JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Track track = createTrack();
        CompletableFuture<HttpResponse<String>> post = restClient.post(track,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + TRACK_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + TRACK_ENDPOINT))
                .withRequestBody(equalToJson(getObjectMapper().writeValueAsString(track))));
    }

    @Test
    void getConfig() throws URISyntaxException {
        RestClient restClient = new RestClient();
        RemoteConfig expectedRemoteConfig = createRemoteConfig();
        AnalyticsRemoteConfig analyticsRemoteConfig = restClient.getConfig(new URI(TEST_CONFIG_URL)).get();
        assertNotNull(analyticsRemoteConfig);
        assertEquals(expectedRemoteConfig.isActive(), analyticsRemoteConfig.isActive());
        assertEquals(expectedRemoteConfig.getDenyAnonymousIds().size(), analyticsRemoteConfig.getDenyAnonymousIds().size());
        assertEquals(expectedRemoteConfig.getDenyQuarkusVersions().size(),
                analyticsRemoteConfig.getDenyQuarkusVersions().size());
        assertEquals(expectedRemoteConfig.getRefreshInterval(), analyticsRemoteConfig.getRefreshInterval());
    }

    private Identity createIdentity() {
        return Identity.builder()
                .context(createContext())
                .userId("12345678901234567890")
                .timestamp(Instant.now()).build();
    }

    private Track createTrack() {
        return Track.builder()
                .userId("12345678901234567890")
                .event(TrackEventType.BUILD)
                .properties(TrackProperties.builder()
                        .appExtensions(List.of(
                                TrackProperties.AppExtension.builder()
                                        .groupId("group1")
                                        .artifactId("artifact1")
                                        .version("1.0").build(),
                                TrackProperties.AppExtension.builder()
                                        .groupId("group2")
                                        .artifactId("artifact2")
                                        .version("2.0").build()))
                        .build())
                .build();
    }
}
