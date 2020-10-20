package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(KeycloakTestResource.class);

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer();
        server.start();

        WireMock.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/certs\"\n"
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

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());
        return Collections.singletonMap("quarkus.oidc.auth-server-url", server.baseUrl() + "/auth/realms/quarkus");
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
