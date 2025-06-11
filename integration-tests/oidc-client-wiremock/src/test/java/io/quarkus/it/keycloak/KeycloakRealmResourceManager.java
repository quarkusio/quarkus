package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.jwt.build.Jwt;

public class KeycloakRealmResourceManager implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(KeycloakRealmResourceManager.class);

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(wireMockConfig().dynamicPort().useChunkedTransferEncoding(ChunkedEncodingPolicy.NEVER));
        server.start();

        server.stubFor(WireMock.post("/tokens")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}")));
        server.stubFor(WireMock.post("/tokens-jwtbearer")
                .withRequestBody(matching("grant_type=client_credentials&"
                        + "client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer&"
                        + "client_assertion=123456"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_jwt_bearer\", \"expires_in\":4, \"refresh_token\":\"refresh_token_jwt_bearer\"}")));
        server.stubFor(WireMock.post("/tokens-jwtbearer-grant")
                .withRequestBody(containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&"
                        + "assertion="))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_jwt_bearer_grant\", \"expires_in\":4, \"refresh_token\":\"refresh_token_jwt_bearer\"}")));
        String jwtBearerToken = Jwt.preferredUserName("Arnold")
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .expiresIn(Duration.ofMinutes(30))
                .signWithSecret("43".repeat(20));
        var jwtBearerTokenPath = Path.of("target").resolve("bearer-token-client-assertion.json");
        try {
            Files.writeString(jwtBearerTokenPath, jwtBearerToken);
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare file with a client assertion", e);
        }
        server.stubFor(WireMock.post("/tokens-jwtbearer-file")
                .withRequestBody(matching("grant_type=client_credentials&"
                        + "client_assertion=" + jwtBearerToken
                        + "&client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_jwt_bearer\", \"expires_in\":4, \"refresh_token\":\"refresh_token_jwt_bearer\"}")));
        server.stubFor(WireMock.post("/tokens_public_client")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice&client_id=quarkus-app"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_public_client\", \"expires_in\":20}")));
        server.stubFor(WireMock.post("/non-standard-tokens")
                .withHeader("X-Custom", matching("XCustomHeaderValue"))
                .withHeader("GrantType", matching("password"))
                .withHeader("client-id", containing("non-standard-response"))
                .withRequestBody(matching("grant_type=password&username=alice&password=alice&extra_param=extra_param_value"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"accessToken\":\"access_token_n\", \"expiresIn\":\"4\", \"refreshToken\":\"refresh_token_n\"}")));

        server.stubFor(WireMock.post("/tokens")
                .withRequestBody(matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_2\", \"expires_in\":4, \"refresh_token\":\"refresh_token_2\", \"refresh_expires_in\":1}")));

        server.stubFor(WireMock.post("/tokens-without-expires-in")
                .withRequestBody(matching("grant_type=client_credentials&client_id=quarkus-app&client_secret=secret"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_without_expires_in\"}")));

        server.stubFor(WireMock.post("/refresh-token-only")
                .withRequestBody(
                        matching("grant_type=refresh_token&refresh_token=shared_refresh_token&extra_param=extra_param_value"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"temp_access_token\", \"expires_in\":4}")));

        server.stubFor(WireMock.post("/ciba-token")
                .withRequestBody(matching(
                        "grant_type=urn%3Aopenid%3Aparams%3Agrant-type%3Aciba&client_id=quarkus-app&client_secret=secret&auth_req_id=16cdaa49-9591-4b63-b188-703fa3b25031"))
                .willReturn(WireMock
                        .badRequest()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"error\":\"expired_token\"}")));
        server.stubFor(WireMock.post("/ciba-token")
                .withRequestBody(matching(
                        "grant_type=urn%3Aopenid%3Aparams%3Agrant-type%3Aciba&client_id=quarkus-app&client_secret=secret&auth_req_id=b1493f2f-c25c-40f5-8d69-94e2ad4b06df"))
                .inScenario("auth-device-approval")
                .whenScenarioStateIs(CibaAuthDeviceApprovalState.PENDING.name())
                .willReturn(WireMock
                        .badRequest()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"error\":\"authorization_pending\"}")));
        server.stubFor(WireMock.post("/ciba-token")
                .withRequestBody(matching(
                        "grant_type=urn%3Aopenid%3Aparams%3Agrant-type%3Aciba&client_id=quarkus-app&client_secret=secret&auth_req_id=b1493f2f-c25c-40f5-8d69-94e2ad4b06df"))
                .inScenario("auth-device-approval")
                .whenScenarioStateIs(CibaAuthDeviceApprovalState.DENIED.name())
                .willReturn(WireMock
                        .badRequest()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"error\":\"access_denied\"}")));
        server.stubFor(WireMock.post("/ciba-token")
                .withRequestBody(matching(
                        "grant_type=urn%3Aopenid%3Aparams%3Agrant-type%3Aciba&client_id=quarkus-app&client_secret=secret&auth_req_id=b1493f2f-c25c-40f5-8d69-94e2ad4b06df"))
                .inScenario("auth-device-approval")
                .whenScenarioStateIs(CibaAuthDeviceApprovalState.APPROVED.name())
                .willReturn(WireMock
                        .ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"ciba_access_token\", \"expires_in\":4, \"refresh_token\":\"ciba_refresh_token\"}")));

        server.stubFor(WireMock.post("/device-token")
                .withRequestBody(matching(
                        "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&client_id=quarkus-app&client_secret=secret&device_code=123456789"))
                .willReturn(WireMock
                        .ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"device_code_access_token\", \"expires_in\":4}")));

        // delay to expand the gap for concurrency tests
        server.stubFor(WireMock.post("/tokens-with-delay")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_1\", \"expires_in\":1, \"refresh_token\":\"refresh_token_1\"}")
                        .withFixedDelay(50)));
        server.stubFor(WireMock.post("/tokens-with-delay")
                .withRequestBody(matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_2\", \"expires_in\":1, \"refresh_token\":\"refresh_token_2\", \"refresh_expires_in\":1}")
                        .withFixedDelay(50)));

        server.stubFor(WireMock.post("/tokens-refresh-test")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody("{\"access_token\":\"access_token_1\", \"expires_in\":3, " +
                                "\"refresh_token\":\"refresh_token_1\", \"refresh_expires_in\": 100}")));
        IntStream.range(0, 20).forEach(i -> {
            int nextIndex = i + 1;
            server.stubFor(WireMock.post("/tokens-refresh-test")
                    .withRequestBody(matching("grant_type=refresh_token&refresh_token=refresh_token_" + i))
                    .willReturn(WireMock
                            .aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                            .withBody("{\"access_token\":\"access_token_" + nextIndex
                                    + "\", \"expires_in\":3, \"refresh_token\":\"refresh_token_"
                                    + nextIndex + "\", \"refresh_expires_in\":100}")));
        });

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());

        Map<String, String> conf = new HashMap<>();
        conf.put("keycloak.url", server.baseUrl());
        conf.put("token-path", jwtBearerTokenPath.toString());
        return conf;
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            LOG.info("Keycloak was shut down");
            server = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(server,
                new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }
}
