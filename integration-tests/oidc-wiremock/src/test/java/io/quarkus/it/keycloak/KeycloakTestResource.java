package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableSet;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(KeycloakTestResource.class);

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/certs\",\n" +
                                        "    \"token_introspection_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\",\n"
                                        + "\"issuer\" : \"https://server.example.com\""
                                        + "}")));

        server.stubFor(
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

        // define the mock for the introspect endpoint

        // Valid
        defineValidIntrospectionMockTokenStubForUserWithRoles("alice", ImmutableSet.of("user"));
        defineValidIntrospectionMockTokenStubForUserWithRoles("jdoe", ImmutableSet.of("user"));
        defineValidIntrospectionMockTokenStubForUserWithRoles("admin", ImmutableSet.of("user", "admin"));

        // Invalid
        defineInvalidIntrospectionMockTokenStubForUserWithRoles("expired", emptySet());

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());
        return Collections.singletonMap("quarkus.oidc.auth-server-url", server.baseUrl() + "/auth/realms/quarkus");
    }

    private void defineValidIntrospectionMockTokenStubForUserWithRoles(String user, Set<String> roles) {
        server.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token/introspect")
                .withRequestBody(matching("token=" + user + "&token_type_hint=access_token"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"active\":true,\"scope\":\"" + roles.stream().collect(joining(" ")) + "\",\"username\":\""
                                        + user
                                        + "\",\"iat\":1,\"exp\":999999999999,\"expires_in\":999999999999,\"client_id\":\"my_client_id\"}")));
    }

    private void defineInvalidIntrospectionMockTokenStubForUserWithRoles(String user, Set<String> roles) {
        server.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token/introspect")
                .withRequestBody(matching("token=" + user + "&token_type_hint=access_token"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(
                                "{\"active\":true,\"scope\":\"" + roles.stream().collect(joining(" ")) + "\",\"username\":\""
                                        + user
                                        + "\",\"iat\":1562315654,\"exp\":1,\"expires_in\":1,\"client_id\":\"my_client_id\"}")));
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
