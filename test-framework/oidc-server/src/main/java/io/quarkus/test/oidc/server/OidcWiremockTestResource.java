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

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.jboss.logging.Logger;
import org.jose4j.keys.X509Util;

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
    private static final String ENCODED_X5C = "MIIC+zCCAeOgAwIBAgIGAXx/E9rgMA0GCSqGSIb3DQEBCwUAMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0yMTEwMTQxMzUzMDBaFw0yMjEwMTQxMzUzMDBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIicN95dXlQLBqEZUsqPhQopnjnPgGmW80NohEgNzZLqN0xW9cyJJrdJM5Z1lRrePHZGiJdd1XXn4fYasP6/cjRfMWal9X6dD5wlnOTP01/4beX5vctE6W4lZrI3kTFmZ+I69w7BaLsUPWgV1CYrtuldL3dr6xAnngK3hU+JraB2Ndw9llXib26HOZhCXKedCTYcUQieVJGPI0f8H1JNk88+PnwI+cUGgXHF56iTLv9QujI6AhIgextXdd21T0XiHgBkSlSSBeqIKAjfCW6zoXP+PJU+Lso24J3duG3mrbilqHZlmIWnLRaG0RmKOeedXIDHvAaMaVUOLaN9HBgNKo0CAwEAAaNTMFEwHQYDVR0OBBYEFMYGoBNHBTMvMT4DwClVHVVwn+5VMB8GA1UdIwQYMBaAFMYGoBNHBTMvMT4DwClVHVVwn+5VMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAFulB0DKhykXGbGPIBPcj63ItLNilgl1i8i43my8fYdV6OBWLIhZ4InhpX1+XmYCNPNtu94Jy1csS00K2/Hhn4ByBd+6nd5DSr0W0VdVQyhLz3GW1nf0J3X2N+tD818O0KtKKPTq4p9reg/XtV+DNv7DeDAGzlfgRL4E4fQx6OYeuu35kGrPvAddIA70leJMELJRylCLfEcl2ne/Bht8cZVp7ZCxnfXnsc+7hCW84mhzGjJycA3E6TnZPD3pD+q9FoIAQMxMQqUCH71u9vTvz1Q5JdokuJJY2eTHSUKyHA9MwSFq8DFDICJFBoQuFyDlK5yxSUcQpR3mBwKdimj6oA0=";

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
                                        "    \"userinfo_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/userinfo\"," +
                                        "    \"token_endpoint\": \"" + server.baseUrl() + "/auth/realms/quarkus/token\"," +
                                        "    \"issuer\" : \"" + TOKEN_ISSUER + "\"," +
                                        "    \"introspection_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/token/introspect\","
                                        + "    \"end_session_endpoint\": \"" + server.baseUrl()
                                        + "/auth/realms/quarkus/protocol/openid-connect/end-session\""
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
                                        "    },\n" +
                                        "    {" +
                                        "      \"kty\": \"RSA\"," +
                                        "      \"alg\": \"RS256\"," +
                                        "      \"n\":\"iJw33l1eVAsGoRlSyo-FCimeOc-AaZbzQ2iESA3Nkuo3TFb1zIkmt0kzlnWVGt48dkaIl13Vdefh9hqw_r9yNF8xZqX1fp0PnCWc5M_TX_ht5fm9y0TpbiVmsjeRMWZn4jr3DsFouxQ9aBXUJiu26V0vd2vrECeeAreFT4mtoHY13D2WVeJvboc5mEJcp50JNhxRCJ5UkY8jR_wfUk2Tzz4-fAj5xQaBccXnqJMu_1C6MjoCEiB7G1d13bVPReIeAGRKVJIF6ogoCN8JbrOhc_48lT4uyjbgnd24beatuKWodmWYhactFobRGYo5551cgMe8BoxpVQ4to30cGA0qjQ\",\n"
                                        +
                                        "      \"e\":\"AQAB\",\n" +
                                        "      \"x5c\": [" +
                                        "          \"" + ENCODED_X5C + "\""
                                        +
                                        "      ]" +
                                        "    }" +
                                        "  ]\n" +
                                        "}")));

        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/single-key-without-kid-thumbprint"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "  \"keys\" : [\n" +
                                        "    {\n" +
                                        "      \"kty\":\"RSA\",\n" +
                                        "      \"n\":\"iJw33l1eVAsGoRlSyo-FCimeOc-AaZbzQ2iESA3Nkuo3TFb1zIkmt0kzlnWVGt48dkaIl13Vdefh9hqw_r9yNF8xZqX1fp0PnCWc5M_TX_ht5fm9y0TpbiVmsjeRMWZn4jr3DsFouxQ9aBXUJiu26V0vd2vrECeeAreFT4mtoHY13D2WVeJvboc5mEJcp50JNhxRCJ5UkY8jR_wfUk2Tzz4-fAj5xQaBccXnqJMu_1C6MjoCEiB7G1d13bVPReIeAGRKVJIF6ogoCN8JbrOhc_48lT4uyjbgnd24beatuKWodmWYhactFobRGYo5551cgMe8BoxpVQ4to30cGA0qjQ\",\n"
                                        +
                                        "      \"e\":\"AQAB\"\n" +
                                        "    }" +
                                        "  ]\n" +
                                        "}")));

        defineUserInfoStubForOpaqueToken("alice");
        defineUserInfoStubForOpaqueToken("admin");
        defineUserInfoStubForJwt();

        // define the mock for the introspect endpoint

        // Valid
        defineValidIntrospectionMockTokenStubForUserWithRoles("alice", ImmutableSet.copyOf(getUserRoles()));
        defineValidIntrospectionMockTokenStubForUserWithRoles("admin", ImmutableSet.copyOf(getAdminRoles()));

        // Invalid
        defineInvalidIntrospectionMockTokenStubForUserWithRoles("expired", emptySet());

        // Code Flow Authorization Mock
        defineCodeFlowAuthorizationMockTokenStub();
        defineCodeFlowAuthorizationMockEncryptedTokenStub();

        //JWT bearer token grant
        defineJwtBearerGrantTokenStub();

        // Login Page
        server.stubFor(
                get(urlPathMatching("/auth/realms/quarkus[/]?"))
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

        // Login Page, form_post response mode
        server.stubFor(
                get(urlPathMatching("/auth/realms/quarkus-form-post[/]?"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "text/html")
                                .withBody("<html>\n" +
                                        "<body>\n" +
                                        " <form action=\"/login-form-post\" name=\"form\">\n" +
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

        // Login Request, form_post response mode
        server.stubFor(
                get(urlPathMatching("/login-form-post"))
                        .willReturn(aResponse()
                                .withBody("<html>\n" +
                                        "   <head><title>Submit This Form</title></head>\n" +
                                        "   <body onload=\"javascript:document.forms[0].submit()\">\n" +
                                        "    <form method=\"post\" action=\"{{request.query.redirect_uri}}\">\n" +
                                        "      <input type=\"hidden\" name=\"state\"\n" +
                                        "       value=\"{{request.query.state}}\"/>\n" +
                                        "      <input type=\"hidden\" name=\"code\"\n" +
                                        "       value=\"58af24f2-9093-4674-a431-4a9d66be719c.50437113-cd78-48a2-838e-b936fe458c5d.0ac5df91-e044-4051-bd03-106a3a5fb9cc\"/>\n"
                                        +
                                        "    </form>\n" +
                                        "   </body>\n" +
                                        "  </html>\n" +
                                        "")
                                .withTransformers("response-template")));

        LOG.infof("Keycloak started in mock mode: %s", server.baseUrl());
        Map<String, String> conf = new HashMap<>();
        conf.put("keycloak.url", server.baseUrl() + "/auth");
        conf.put("smallrye.jwt.sign.key.location", "privateKey.jwk");

        return conf;
    }

    private void defineUserInfoStubForOpaqueToken(String user) {
        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/protocol/openid-connect/userinfo"))
                        .withHeader("Authorization", matching("Bearer " + user))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "      \"preferred_username\": \"" + user + "\""
                                        + "}")));
    }

    private void defineUserInfoStubForJwt() {
        server.stubFor(
                get(urlEqualTo("/auth/realms/quarkus/protocol/openid-connect/userinfo"))
                        .withHeader("Authorization", containing("Bearer ey"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\n" +
                                        "      \"preferred_username\": \"alice\""
                                        + "}")));
    }

    private void defineValidIntrospectionMockTokenStubForUserWithRoles(String user, Set<String> roles) {
        long exp = now() + 300;
        server.stubFor(WireMock.post("/auth/realms/quarkus/protocol/openid-connect/token/introspect")
                .withRequestBody(matching("token=" + user + "&token_type_hint=access_token"))
                .willReturn(WireMock
                        .aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"active\":true,\"scope\":\"" + roles.stream().collect(joining(" ")) + "\",\"username\":\""
                                        + user
                                        + "\",\"iat\":1,\"exp\":" + exp + ",\"expires_in\":" + exp
                                        + ",\"client_id\":\"my_client_id\"}")));
    }

    private static final long now() {
        return System.currentTimeMillis();
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

    private void defineJwtBearerGrantTokenStub() {
        server.stubFor(WireMock.post("/auth/realms/quarkus/jwt-bearer-token")
                .withRequestBody(containing("client_id=quarkus-app"))
                .withRequestBody(containing("client_secret=secret"))
                .withRequestBody(containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"))
                .withRequestBody(containing("scope=https%3A%2F%2Fgraph.microsoft.com%2Fuser.read+offline_access"))
                .withRequestBody(containing("requested_token_use=on_behalf_of"))
                .withRequestBody(containing("assertion"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + getAccessToken("bob", getUserRoles()) + "\""
                                + "}")));
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

    private void defineCodeFlowAuthorizationMockEncryptedTokenStub() {
        server.stubFor(WireMock.post("/auth/realms/quarkus/encrypted-id-token")
                .withRequestBody(containing("authorization_code"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"access_token\": \""
                                + getAccessToken("alice", getAdminRoles()) + "\",\n" +
                                "  \"refresh_token\": \"07e08903-1263-4dd1-9fd1-4a59b0db5283\",\n" +
                                "  \"id_token\": \"" + getEncryptedIdToken("alice", getAdminRoles())
                                + "\"\n" +
                                "}")));
    }

    public static String getEncryptedIdToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer(TOKEN_ISSUER)
                .audience(TOKEN_AUDIENCE)
                .subject("123456")
                .jws()
                .keyId("1")
                .innerSign("privateKey.jwk").encrypt("publicKey.jwk");
    }

    public static X509Certificate getCertificate() {
        try {
            return new X509Util().fromBase64Der(ENCODED_X5C);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Set<String> getAdminRoles() {
        return new HashSet<>(Arrays.asList(TOKEN_ADMIN_ROLES.split(",")));
    }

    private Set<String> getUserRoles() {
        return new HashSet<>(Arrays.asList(TOKEN_USER_ROLES.split(",")));
    }

    public static String getAccessToken(String userName, Set<String> groups) {
        return generateJwtToken(userName, groups);
    }

    public static String getIdToken(String userName, Set<String> groups) {
        return generateJwtToken(userName, groups);
    }

    public static String generateJwtToken(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer(TOKEN_ISSUER)
                .audience(TOKEN_AUDIENCE)
                .claim("sid", "session-id")
                .subject("123456")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    public static String getLogoutToken() {
        return Jwt.issuer(TOKEN_ISSUER)
                .audience(TOKEN_AUDIENCE)
                .subject("123456")
                .claim("events", createEventsClaim())
                .claim("sid", "session-id")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    public static String getLogoutToken(String sub) {
        return Jwt.issuer(TOKEN_ISSUER)
                .audience(TOKEN_AUDIENCE)
                .subject(sub)
                .claim("events", createEventsClaim())
                .claim("sid", "session-id")
                .jws()
                .keyId("1")
                .sign("privateKey.jwk");
    }

    private static JsonObject createEventsClaim() {
        return Json.createObjectBuilder().add("http://schemas.openid.net/event/backchannel-logout",
                Json.createObjectBuilder().build()).build();
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
