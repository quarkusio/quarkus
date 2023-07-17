package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

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
        server.stubFor(WireMock.post("/tokens_public_client")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice&client_id=quarkus-app"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_public_client\", \"expires_in\":20}")));
        server.stubFor(WireMock.post("/non-standard-tokens")
                .withHeader("X-Custom", matching("XCustomHeaderValue"))
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

        server.stubFor(WireMock.post("/refresh-token-only")
                .withRequestBody(matching("grant_type=refresh_token&refresh_token=shared_refresh_token"))
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

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());

        Map<String, String> conf = new HashMap<>();
        conf.put("keycloak.url", server.baseUrl());
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
