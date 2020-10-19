package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(options().port(8180));
        server.start();

        WireMock.configureFor(8180);
        WireMock.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "    \"authorization_endpoint\": \"http://localhost:8180/authenticate\",\n" +
                                        "    \"end_session_endpoint\": \"http://localhost:8180/logout\",\n" +
                                        "    \"id_token_signing_alg_values_supported\": [\n" +
                                        "        \"RS256\",\n" +
                                        "        \"ES256\",\n" +
                                        "        \"HS256\"\n" +
                                        "    ],\n" +
                                        "    \"issuer\": \"http://localhost:8180/auth/realms/quarkus\",\n" +
                                        "    \"jwks_uri\": \"http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/certs\",\n"
                                        +
                                        "    \"response_types_supported\": [\n" +
                                        "        \"code\",\n" +
                                        "        \"code id_token\",\n" +
                                        "        \"id_token\",\n" +
                                        "        \"token id_token\"\n" +
                                        "    ],\n" +
                                        "    \"subject_types_supported\": [\n" +
                                        "        \"public\"\n" +
                                        "    ],\n" +
                                        "    \"token_endpoint\": \"http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/token\"\n"
                                        +
                                        "}")));

        WireMock.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/protocol/openid-connect/certs"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "  \"keys\" : [\n" +
                                        "    {\n" +
                                        "      \"kid\": \"1\",\n" +
                                        "      \"kty\":\"RSA\",\n" +
                                        "      \"n\":\"iJw33l1eVAsGoRlSyo-FCimeOc-AaZbzQ2iESA3Nkuo3TFb1zIkmt0kzlnWVGt48dkaIl13Vdefh9hqw_r9yNF8xZqX1fp0PnCWc5M_TX_ht5fm9y0TpbiVmsjeRMWZn4jr3DsFouxQ9aBXUJiu26V0vd2vrECeeAreFT4mtoHY13D2WVeJvboc5mEJcp50JNhxRCJ5UkY8jR_wfUk2Tzz4-fAj5xQaBccXnqJMu_1C6MjoCEiB7G1d13bVPReIeAGRKVJIF6ogoCN8JbrOhc_48lT4uyjbgnd24beatuKWodmWYhactFobRGYo5551cgMe8BoxpVQ4to30cGA0qjQ\",\n"
                                        +
                                        "      \"e\":\"AQAB\"\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}")));

        System.out.println("[INFO] Keycloak started in mock mode: " + server.baseUrl());
        return Collections.singletonMap("quarkus.oidc.auth-server-url", server.baseUrl() + "/auth/realms/quarkus");
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            System.out.println("[INFO] Keycloak was shut down");
            server = null;
        }
    }

}
