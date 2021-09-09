package io.quarkus.test.oidc.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.common.collect.ImmutableSet;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.jwt.build.Jwt;

/**
 * Provides a mock OIDC server to tests.
 *
 * @see OidcWireMock
 */
public class OidcWiremockTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(OidcWiremockTestResource.class);
    private static final String TOKEN_ISSUER = System.getProperty("quarkus.test.oidc.token.issuer",
            "https://server.example.com");
    private static final String TOKEN_AUDIENCE = System.getProperty("quarkus.test.oidc.token.audience",
            "https://server.example.com");
    private static final String TOKEN_USER_ROLES = System.getProperty("quarkus.test.oidc.token.user-roles", "user");
    private static final String TOKEN_ADMIN_ROLES = System.getProperty("quarkus.test.oidc.token.admin-roles", "user,admin");

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(
                wireMockConfig()
                        .extensions(new ResponseTemplateTransformer(false))
                        .dynamicPort());
        server.start();

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "    \"jwks_uri\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/certs\",\n" +
                                        "    \"token_introspection_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\",\n" +
                                        "    \"authorization_endpoint\": \"" + server.baseUrl() + "/auth/realms/quarkus\"," +
                                        "    \"token_endpoint\": \"" + server.baseUrl() + "/auth/realms/quarkus/token\"," +
                                        "    \"issuer\" : \"" + TOKEN_ISSUER + "\"," +
                                        "    \"introspection_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\""
                                        +
                                        "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/protocol/openid-connect/certs"))
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

        // define the mock for the introspect endpoint

        // Valid
        defineValidIntrospectionMockTokenStubForUserWithRoles("alice", ImmutableSet.copyOf(getUserRoles()));
        defineValidIntrospectionMockTokenStubForUserWithRoles("admin", ImmutableSet.copyOf(getAdminRoles()));

        // Invalid
        defineInvalidIntrospectionMockTokenStubForUserWithRoles("expired", emptySet());

        // Code Flow Authorization Mock
        defineCodeFlowAuthorizationMockTokenStub();

        // Login Page
        server.stubFor(
                get(urlPathMatching("/auth/realms/quarkus"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "text/html")
                                .withBody("<html>\n" +
                                        "<body>\n" +
                                        " <form action=\"/login\" name=\"form\">\n" +
                                        "  <input type=\"text\" id=\"username\" name=\"username\"/>\n" +
                                        "  <input type=\"password\" id=\"password\" name=\"password\"/>\n" +
                                        "  <input type=\"hidden\" id=\"state\" name=\"state\" value=\"{{request.query.state}}\"/>\n"
                                        +
                                        "  <input type=\"hidden\" id=\"redirect_uri\" name=\"redirect_uri\" value=\"{{request.query.redirect_uri}}\"/>\n"
                                        +
                                        "  <input type=\"submit\" id=\"login\" value=\"login\"/>\n" +
                                        "</form>\n" +
                                        "</body>\n" +
                                        "</html> ")
                                .withTransformers("response-template")));

        // Login Request
        server.stubFor(
                get(urlPathMatching("/login"))
                        .willReturn(aResponse()
                                .withHeader("Location",
                                        "{{request.query.redirect_uri}}?state={{request.query.state}}&code=58af24f2-9093-4674-a431-4a9d66be719c.50437113-cd78-48a2-838e-b936fe458c5d.0ac5df91-e044-4051-bd03-106a3a5fb9cc")
                                .withStatus(302)
                                .withTransformers("response-template")));

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());
        Map<String, String> conf = new HashMap<>();
        conf.put("keycloak.url", server.baseUrl() + "/auth");

        return conf;
    }

    private void defineValidIntrospectionMockTokenStubForUserWithRoles(String user, Set<String> roles) {
        server.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token/introspect")
                .withRequestBody(matching("token=" + user + "&token_type_hint=access_token"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
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
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"active\":true,\"scope\":\"" + roles.stream().collect(joining(" ")) + "\",\"username\":\""
                                        + user
                                        + "\",\"iat\":1562315654,\"exp\":1,\"expires_in\":1,\"client_id\":\"my_client_id\"}")));
    }

    private void defineCodeFlowAuthorizationMockTokenStub() {
        server.stubFor(WireMock.post("/auth/realms/quarkus/token")
                .withRequestBody(containing("authorization_code"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + getAccessToken("alice", getAdminRoles()) + "\",\n" +
                                "  \"refresh_token\": \"07e08903-1263-4dd1-9fd1-4a59b0db5283\",\n" +
                                "  \"id_token\": \"" + getIdToken("alice", getAdminRoles())
                                + "\"\n" +
                                "}")));
    }

    private Set<String> getAdminRoles() {
        return new HashSet<>(Arrays.asList(TOKEN_ADMIN_ROLES.split(",")));
    }

    private Set<String> getUserRoles() {
        return new HashSet<>(Arrays.asList(TOKEN_USER_ROLES.split(",")));
    }

    private String getAccessToken(String userName, Set<String> groups) {
        return generateJwtToken(userName, groups);
    }

    private String getIdToken(String userName, Set<String> groups) {
        return generateJwtToken(userName, groups);
    }

    private String generateJwtToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer(TOKEN_ISSUER)
                .audience(TOKEN_AUDIENCE)
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(server,
                new TestInjector.AnnotatedAndMatchesType(OidcWireMock.class, WireMockServer.class));
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
