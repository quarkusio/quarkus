package io.quarkus.analytics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_APP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_BUILD;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_CI;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRAALVM;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_IP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_KUBERNETES;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_LOCATION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_MAVEN_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_NAME;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_OS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_QUARKUS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VENDOR;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_DISTRIBUTION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_JAVA;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.MAVEN_VERSION;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.TestFileLocationsImpl;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.analytics.dto.segment.TrackProperties;
import io.quarkus.devtools.messagewriter.MessageWriter;

class AnalyticsServiceTest extends AnalyticsServiceTestBase {

    private static final int MOCK_SERVER_PORT = 9300;
    private static final String TEST_CONFIG_URL = "http://localhost:" + MOCK_SERVER_PORT + "/" + "config";
    private static final WireMockServer wireMockServer = new WireMockServer(MOCK_SERVER_PORT);
    private static FileLocations FILE_LOCATIONS;

    @BeforeAll
    static void start() throws IOException {
        FILE_LOCATIONS = new TestFileLocationsImpl();
        System.setProperty("quarkus.analytics.uri.base", "http://localhost:" + MOCK_SERVER_PORT + "/");
        wireMockServer.start();
        wireMockServer.stubFor(post(urlEqualTo("/v1/identify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/v1/track"))
                .willReturn(aResponse()
                        .withStatus(200)
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

    @AfterAll
    static void stop() throws IOException {
        wireMockServer.stop();
        System.clearProperty("quarkus.analytics.uri.base");
        ((TestFileLocationsImpl) FILE_LOCATIONS).deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createContext() throws IOException {
        AnalyticsService service = new AnalyticsService(FILE_LOCATIONS, MessageWriter.info());

        final Map<String, Object> contextMap = service.createContextMap(
                mockApplicationModel(),
                Map.of(GRAALVM_VERSION_DISTRIBUTION, "Company name",
                        GRAALVM_VERSION_VERSION, "20.2.0",
                        GRAALVM_VERSION_JAVA, "17.0.0",
                        MAVEN_VERSION, "3.9.0",
                        GRADLE_VERSION, "8.0.1"));

        assertNotNull(contextMap);
        final Map app = (Map) contextMap.get(PROP_APP);
        assertNotNull(app);
        assertEquals("yRAqgUsoDknuOICn/0zeC14YwZYAxPxcycCw6MgGYfI=", app.get(PROP_NAME));
        assertEquals("Uue4h73VUgajaMLTPcYAM4Fo+YAZx4LQ0OEdaBbQTtg=", app.get(PROP_VERSION));
        assertMapEntriesNotEmpty(1, (Map) contextMap.get(PROP_KUBERNETES));
        assertMapEntriesNotEmpty(1, (Map) contextMap.get(PROP_CI));
        final Map java = (Map) contextMap.get(PROP_JAVA);
        assertNotNull(java);
        assertNotNull(java.get(PROP_VENDOR));
        assertNotNull(java.get(PROP_VERSION));
        assertMapEntriesNotEmpty(3, (Map) contextMap.get(PROP_OS));
        final Map build = (Map) contextMap.get(PROP_BUILD);
        assertNotNull(build);
        // in reality, these are not both set at the same time, but we set them in the test
        assertEquals("3.9.0", build.get(PROP_MAVEN_VERSION));
        assertEquals("8.0.1", build.get(PROP_GRADLE_VERSION));
        final Map graalvm = (Map) contextMap.get(PROP_GRAALVM);
        assertNotNull(graalvm);
        assertEquals("Company name", graalvm.get(PROP_VENDOR));
        assertEquals("20.2.0", graalvm.get(PROP_VERSION));
        assertEquals("17.0.0", graalvm.get(PROP_JAVA_VERSION));
        assertNotNull(contextMap.get("timezone"));
        assertMapEntriesNotEmpty(1, (Map) contextMap.get(PROP_QUARKUS));
        assertEquals("0.0.0.0", contextMap.get(PROP_IP));
        assertNotNull(contextMap.get(PROP_LOCATION));
    }

    @Test
    void createExtensionsPropertyValue() {
        AnalyticsService service = new AnalyticsService(FILE_LOCATIONS, MessageWriter.info());
        List<TrackProperties.AppExtension> extensionsPropertyValue = service
                .createExtensionsPropertyValue(mockApplicationModel());

        assertNotNull(extensionsPropertyValue);
        assertEquals(2, extensionsPropertyValue.size());
        assertEquals(Set.of("quarkus-openapi", "quarkus-opentelemetry-jaeger"),
                extensionsPropertyValue.stream()
                        .map(TrackProperties.AppExtension::getArtifactId)
                        .collect(Collectors.toSet()));
    }

    @Test
    void sendAnalyticsTest() throws IOException {
        AnalyticsService service = new AnalyticsService(FILE_LOCATIONS, MessageWriter.info());
        service.sendAnalytics(TrackEventType.BUILD,
                mockApplicationModel(),
                Map.of(),
                new File(FILE_LOCATIONS.getFolder().toUri()));
        service.close();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlEqualTo("/v1/track"))));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/track"))
                .withRequestBody(notMatching("null")));
        assertTrue(new File(FILE_LOCATIONS.getFolder().toString() + "/" + FILE_LOCATIONS.lastTrackFileName()).exists());
    }

    @Test
    void nullLogger() {
        AnalyticsService service = new AnalyticsService(FILE_LOCATIONS, null);
        assertNotNull(service);
        service.sendAnalytics(TrackEventType.BUILD,
                mockApplicationModel(),
                Map.of(),
                new File(FILE_LOCATIONS.getFolder().toUri()));
    }

    private void assertMapEntriesNotEmpty(int size, Map<String, Object> map) {
        assertNotNull(map);
        assertEquals(size, map.size());
        map.entrySet().forEach(entry -> {
            assertNotNull(entry.getValue());
            assertFalse(entry.getValue().toString().isEmpty(), entry.toString() + " value is empty");
        });
    }
}
