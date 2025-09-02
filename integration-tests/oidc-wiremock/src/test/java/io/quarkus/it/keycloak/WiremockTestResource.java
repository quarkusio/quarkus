package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.not;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.quarkus.oidc.OidcConfigurationMetadata.ISSUER;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;

public class WiremockTestResource {

    private static final Logger LOG = Logger.getLogger(WiremockTestResource.class);

    private final String issuer;
    private final int port;
    private WireMockServer server;

    public WiremockTestResource() {
        this.issuer = null;
        this.port = 8180;
    }

    public WiremockTestResource(String issuer, int port) {
        this.issuer = issuer;
        this.port = port;
    }

    public void start() {

        server = new WireMockServer(
                wireMockConfig()
                        .port(port));
        server.start();

        server.stubFor(
                head(urlEqualTo("/auth/realms/quarkus2/.well-known/openid-configuration"))
                        .willReturn(aResponse().withStatus(200)));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus2/.well-known/openid-configuration"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("Cookie", absent())
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Location", "http://localhost:" + port
                                        + "/auth/realms/quarkus2/.well-known/openid-configuration")
                                .withHeader("Set-Cookie", "redirect=true; Path=/; Domain=some.domain.com")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus2/.well-known/openid-configuration"))
                        .withHeader("Cookie", equalTo("redirect=true"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        (issuer == null ? "" : " \"" + ISSUER + "\": " + "\"" + issuer + "\",\n") +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus2/protocol/openid-connect/certs\""
                                        + "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus2/protocol/openid-connect/certs"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
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

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus3/.well-known/openid-configuration"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("Cookie", absent())
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Location", "http://localhost:" + port
                                        + "/auth/realms/quarkus3/.well-known/openid-configuration")
                                .withHeader("Set-Cookie", "redirect1=true; Path=/; Domain=some.domain.com")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus3/.well-known/openid-configuration"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("Cookie", equalTo("redirect1=true"))
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Location", "http://localhost:" + port
                                        + "/auth/realms/quarkus3/.well-known/openid-configuration")
                                .withHeader("Set-Cookie", "redirect2=true; Path=/; Domain=some.domain.com")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus3/.well-known/openid-configuration"))
                        .withHeader("Cookie", equalTo("redirect2=true"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        (issuer == null ? "" : " \"" + ISSUER + "\": " + "\"" + issuer + "\",\n") +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus3/protocol/openid-connect/certs\""
                                        + "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus3/protocol/openid-connect/certs"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
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

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus4/.well-known/openid-configuration"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("Cookie", absent())
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Location", "http://localhost:" + port
                                        + "/auth/realms/quarkus4/.well-known/different-openid-configuration")
                                .withHeader("Set-Cookie", "redirect=true; Path=/; Domain=some.domain.com")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus4/.well-known/different-openid-configuration"))
                        .withHeader("Cookie", equalTo("redirect=true"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        (issuer == null ? "" : " \"" + ISSUER + "\": " + "\"" + issuer + "\",\n") +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus4/protocol/openid-connect/certs\""
                                        + "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus4/protocol/openid-connect/certs"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
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

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus5/.well-known/openid-configuration"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("Cookie", absent())
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Location", "http://localhost:" + port
                                        + "/auth/realms/quarkus5/.well-known/openid-configuration")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus5/protocol/openid-connect/certs"))
                        .withHeader("Filter", equalTo("OK"))
                        .withHeader("tenant-id", not(absent()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
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
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop();
            LOG.info("Keycloak was shut down");
            server = null;
        }
    }

}
