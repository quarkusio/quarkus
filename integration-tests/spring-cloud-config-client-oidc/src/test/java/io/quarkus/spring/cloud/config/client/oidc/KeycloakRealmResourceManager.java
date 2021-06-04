package io.quarkus.spring.cloud.config.client.oidc;

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
                .withRequestBody(matching("grant_type=client_credentials&client_id=quarkus-app&client_secret=alice"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}")));
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
