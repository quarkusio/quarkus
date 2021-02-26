package io.quarkus.vault;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WiremockVault implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(wireMockConfig().httpsPort(8201));
        server.start();

        server.stubFor(get(urlEqualTo("/v1/secret/foo"))
                .withHeader("X-Vault-Namespace", equalTo("accounting"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"request_id\":\"bf5245f4-f194-2b13-80b7-6cad145b8135\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"wrap_info\":null,\"warnings\":null,\"auth\":null,"
                                        + "\"data\":{\"hello\":\"world\"}}")));

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
