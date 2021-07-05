package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

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
        server.stubFor(WireMock.post("/non-standard-tokens")
                .withRequestBody(matching("grant_type=password&username=alice&password=alice"))
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
                                "{\"access_token\":\"access_token_2\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}")));

        server.stubFor(WireMock.post("/refresh-token-only")
                .withRequestBody(matching("grant_type=refresh_token&refresh_token=shared_refresh_token"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"temp_access_token\", \"expires_in\":4}")));

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
}
