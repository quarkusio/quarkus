package io.quarkus.it.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.smallrye.jwt.build.Jwt;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

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
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\"," +
                                        "    \"end_session_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/{realm-name}/protocol/openid-connect/logout\"," +
                                        "\"uriTemplate\" : \"test\""
                                        +
                                        "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/.well-known/uma2-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "    \"token_introspection_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\",\n" +
                                        "    \"token_endpoint\": \"" + server.baseUrl() + "/auth/realms/quarkus/token\"," +
                                        "    \"resource_registration_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/authz/protection/resource_set\"," +
                                        "    \"permission_endpoint\" : \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/authz/protection/permission\"," +
                                        "    \"policy_endpoint\" : \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/authz/protection/uma-policy\"" +
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
        //        defineValidMockTokenStubForUserWithRoles("alice", ImmutableSet.copyOf(getUserRoles()));
        defineValidMockTokenStubForUserWithRoles("alice", ImmutableSet.copyOf(getUserRoles()));
        defineValidMockTokenStubForUserWithRoles("admin", ImmutableSet.copyOf(getAdminRoles()));

        // Invalid
        defineInvalidIntrospectionMockTokenStubForUserWithRoles("expired", emptySet());

        // Realm Representation
        defineRealmRepresentation();

        // Code Flow Authorization Mock
        defineCodeFlowAuthorizationMockTokenStub();

        // Resource Set Mock
        defineResourceSetMock();
        //defineResourceSetMock2();

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
        conf.put("quarkus.oidc.auth-server-url", server.baseUrl() + "/auth/realms/quarkus");
        conf.put("quarkus.oidc.tenant.auth-server-url", server.baseUrl() + "/auth/realms/quarkus");
        conf.put("keycloak.url", server.baseUrl() + "/auth");
        return conf;
    }

    private void defineRealmRepresentation() {
        server.stubFor(WireMock.get("/auth/admin/realms/quarkus")
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"realm\": \"quarkus\",\n" +
                                "    \"enabled\": true,\n" +
                                "    \"sslRequired\": \"external\",\n" +
                                "    \"registrationAllowed\": true,\n" +
                                "    \"privateKey\": \"MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=\",\n"
                                +
                                "    \"publicKey\": \"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB\",\n"
                                +
                                "    \"requiredCredentials\": [ \"password\" ],\n" +
                                "    \"users\" : [\n" +
                                "        {\n" +
                                "            \"username\" : \"user\",\n" +
                                "            \"enabled\": true,\n" +
                                "            \"email\" : \"sample-user@example\",\n" +
                                "            \"firstName\": \"Sample\",\n" +
                                "            \"lastName\": \"User\",\n" +
                                "            \"credentials\" : [\n" +
                                "                { \"type\" : \"password\",\n" +
                                "                  \"value\" : \"password\" }\n" +
                                "            ],\n" +
                                "            \"realmRoles\": [ \"user\" ],\n" +
                                "            \"clientRoles\": {\n" +
                                "                \"account\": [\"view-profile\", \"manage-account\"]\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"roles\" : {\n" +
                                "        \"realm\" : [\n" +
                                "            {\n" +
                                "                \"name\": \"user\",\n" +
                                "                \"description\": \"User privileges\"\n" +
                                "            },\n" +
                                "            {\n" +
                                "                \"name\": \"admin\",\n" +
                                "                \"description\": \"Administrator privileges\"\n" +
                                "            }\n" +
                                "        ]\n" +
                                "    },\n" +
                                "    \"scopeMappings\": [\n" +
                                "        {\n" +
                                "            \"client\": \"js-console\",\n" +
                                "            \"roles\": [\"user\"]\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"clients\": [\n" +
                                "        {\n" +
                                "            \"clientId\": \"js-console\",\n" +
                                "            \"enabled\": true,\n" +
                                "            \"publicClient\": true,\n" +
                                "            \"baseUrl\": \"/js-console\",\n" +
                                "            \"redirectUris\": [\n" +
                                "                \"/js-console/*\"\n" +
                                "            ],\n" +
                                "            \"webOrigins\": []\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"clientScopeMappings\": {\n" +
                                "        \"account\": [\n" +
                                "            {\n" +
                                "                \"client\": \"js-console\",\n" +
                                "                \"roles\": [\"view-profile\"]\n" +
                                "            }\n" +
                                "        ]\n" +
                                "    }\n" +
                                "}\n")));
    }

    private void defineValidMockTokenStubForUserWithRoles(String user, ImmutableSet<String> roles) {
        server.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token")
                .withRequestBody(matching("grant_type=password&username=admin&password=admin&client_id=admin-cli"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + getAccessToken(user, getAdminRoles()) + "\",\n" +
                                "  \"refresh_token\": \"07e08903-1263-4dd1-9fd1-4a59b0db5283\",\n" +
                                "  \"id_token\": \"" + getIdToken(user, getAdminRoles())
                                + "\"\n" +
                                "}")));
    }

    private void defineResourceSetMock() {
        server.stubFor(WireMock.get(
                //                "/auth/realms/quarkus/authz/protection/resource_set?owner=quarkus-app&matchingUri=false&deep=true&max=-1&name=Permission+Resource&exactName=true")
                urlPathMatching("/auth/realms/quarkus/authz/protection/resource_set"))
                .willReturn(WireMock
                        .aResponse()
                        .withBody("[{  \n" +
                                "   \"_id\":\"KX3A-39WE\",\n" +
                                "   \"resource_scopes\":[  \n" +
                                "      \"read-public\",\n" +
                                "      \"post-updates\",\n" +
                                "      \"read-private\",\n" +
                                "      \"http://www.example.com/scopes/all\"\n" +
                                "   ],\n" +
                                "   \"uris\":[  \n" +
                                "      \"localhost\"" +
                                "   ],\n" +
                                "   \"icon_uri\":\"http://www.example.com/icons/sharesocial.png\",\n" +
                                "   \"name\":\"Tweedl Social Service\",\n" +
                                "   \"type\":\"http://www.example.com/rsrcs/socialstream/140-compatible\"\n" +
                                "}]")));
    }

    //    private void defineResourceSetMock2() {
    //        server.stubFor(WireMock.get(
    //                "/auth/realms/quarkus/authz/protection/resource_set?matchingUri=false&deep=true&max=-1&exactName=false&uri=%2api%2permission%2claim-protected")
    //                .willReturn(WireMock
    //                        .aResponse()
    //                        .withBody("[{  \n" +
    //                                "   \"_id\":\"KX3A-39WE\",\n" +
    //                                "   \"resource_scopes\":[  \n" +
    //                                "      \"read-public\",\n" +
    //                                "      \"post-updates\",\n" +
    //                                "      \"read-private\",\n" +
    //                                "      \"http://www.example.com/scopes/all\"\n" +
    //                                "   ],\n" +
    //                                "   \"icon_uri\":\"http://www.example.com/icons/sharesocial.png\",\n" +
    //                                "   \"name\":\"Tweedl Social Service\",\n" +
    //                                "   \"type\":\"http://www.example.com/rsrcs/socialstream/140-compatible\"\n" +
    //                                "}]")));
    //    }

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
                //                .withRequestBody(containing("grant_type=client_credentials"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \"" + getIdToken("pat", getAdminRoles()) + "\",\n" +
                                "  \"expires_in\": 300,\n" +
                                "  \"refresh_expires_in\": 1800,\n" +
                                "  \"refresh_token\": \"07e08903-1263-4dd1-9fd1-4a59b0db5283\",\n" +
                                "  \"token_type\": \"bearer\",\n" +
                                "  \"id_token\": \"" + getIdToken("alice", getAdminRoles()) + "\",\n" +
                                "  \"not-before-policy\": 0,\n" +
                                "  \"session_state\": \"ccea4a55-9aec-4024-b11c-44f6f168439e\"\n" +
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
    public synchronized void stop() {
        if (server != null) {
            server.stop();
            LOG.info("Keycloak was shut down");
            server = null;
        }
    }

}
