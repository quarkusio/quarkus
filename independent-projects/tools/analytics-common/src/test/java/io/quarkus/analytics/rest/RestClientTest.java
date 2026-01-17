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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;
import io.quarkus.analytics.dto.config.Identity;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.dto.segment.Track;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.analytics.dto.segment.TrackProperties;
import io.quarkus.analytics.util.JsonSerializer;

class RestClientTest {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final String TEST_CONFIG_URL = "http://localhost:" + MOCK_SERVER_PORT + "/" + "config";
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);

    @BeforeAll
    static void start() {
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

    @BeforeEach
    void reset() {
        wireMockServer.resetRequests();
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
            throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Identity identity = createIdentity();
        CompletableFuture<HttpResponse<String>> post = restClient.post(identity,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + IDENTITY_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .withRequestBody(equalToJson(JsonSerializer.toJson(identity))));
    }

    @Test
    void postIdentity_expectedJson()
            throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Identity identity = createIdentity();
        CompletableFuture<HttpResponse<String>> post = restClient.post(identity,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + IDENTITY_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + IDENTITY_ENDPOINT))
                .withRequestBody(equalToJson(getExpectedIdentity())));
    }

    @Test
    void postTrace()
            throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Track track = createTrack();
        CompletableFuture<HttpResponse<String>> post = restClient.post(track,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + TRACK_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + TRACK_ENDPOINT))
                .withRequestBody(equalToJson(JsonSerializer.toJson(track))));
    }

    @Test
    void postTrace_expectedJson() throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException {
        RestClient restClient = new RestClient();
        Track track = createTrack();
        CompletableFuture<HttpResponse<String>> post = restClient.post(track,
                new URI("http://localhost:" + MOCK_SERVER_PORT + "/" + TRACK_ENDPOINT));
        assertEquals(201, post.get(1, TimeUnit.SECONDS).statusCode());
        wireMockServer.verify(postRequestedFor(urlEqualTo("/" + TRACK_ENDPOINT))
                .withRequestBody(equalToJson(getExpectedTrackJson())));
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
                .timestamp(Instant.MIN).build();
    }

    private Track createTrack() {
        return Track.builder()
                .userId("12345678901234567890")
                .event(TrackEventType.BUILD)
                .context(Map.of("key", "value"))
                .timestamp(Instant.MIN)
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

    private String getExpectedTrackJson() {
        return """
                {
                  "event" : "BUILD",
                  "properties" : {
                    "app_extensions" : [ {
                      "version" : "1.0",
                      "group_id" : "group1",
                      "artifact_id" : "artifact1"
                    }, {
                      "version" : "2.0",
                      "group_id" : "group2",
                      "artifact_id" : "artifact2"
                    } ]
                  },
                  "context" : {
                    "key" : "value"
                  },
                  "timestamp" : "-1000000000-01-01T00:00:00Z",
                  "userId" : "12345678901234567890"
                }
                """;
    }

    private String getExpectedIdentity() {
        return """
                {
                  "context" : {
                    "app" : {
                      "name" : "app-name"
                    },
                    "java" : {
                      "vendor" : "Eclipse",
                      "version" : "17"
                    },
                    "os" : {
                      "name" : "arm64",
                      "os_arch" : "MacOs",
                      "version" : "1234"
                    },
                    "build" : {
                      "gradle_version" : "N/A",
                      "maven_version" : "3.8,1"
                    },
                    "graalvm" : {
                      "vendor" : "N/A",
                      "java_version" : "N/A",
                      "version" : "N/A"
                    },
                    "timezone" : "Europe/Lisbon",
                    "quarkus" : {
                      "version" : "N/A"
                    },
                    "ip" : "0.0.0.0",
                    "location" : {
                      "locale_country" : "Portugal"
                    }
                  },
                  "timestamp" : "-1000000000-01-01T00:00:00Z",
                  "userId" : "12345678901234567890"
                }
                """;
    }
}
