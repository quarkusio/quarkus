package io.quarkus.it.oidc.providers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWireMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;

@QuarkusTest
@QuarkusTestResource(OidcWiremockTestResource.class)
public class OidcSlackProviderTest {

    @OidcWireMock
    WireMockServer wireMockServer;

    @Test
    public void testSlackWellKnownProvider() throws IOException {
        configSlackProviderStubs();
        try (var webClient = createWebClient()) {
            var page = webClient.getPage("http://localhost:8081/slack");
            var responseContent = page.getWebResponse().getContentAsString();
            assertTrue(responseContent.contains("\"userPrincipalName\":\"Rosetta\""), responseContent);
            assertTrue(responseContent.contains("\"userInfoEmail\":\"example@example.com\""), responseContent);
            webClient.getCookieManager().clearCookies();
        }
    }

    private void configSlackProviderStubs() {
        wireMockServer.stubFor(
                get(urlMatching("/auth/slack/.well-known/openid-configuration.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                        """
                                                {
                                                    "issuer": "https://server.example.com",
                                                    "authorization_endpoint": "%1$s/auth/slack/openid/connect/authorize",
                                                    "token_endpoint": "%1$s/auth/slack/api/openid.connect.token",
                                                    "userinfo_endpoint": "%1$s/auth/slack/api/openid.connect.userInfo",
                                                    "jwks_uri": "%1$s/auth/slack/openid/connect/keys",
                                                    "scopes_supported": ["openid","profile","email"],
                                                    "response_types_supported": ["code"],
                                                    "response_modes_supported": ["form_post"],
                                                    "grant_types_supported": ["authorization_code"],
                                                    "subject_types_supported": ["public"],
                                                    "id_token_signing_alg_values_supported": ["RS256"],
                                                    "claims_supported": ["sub","auth_time","iss"],
                                                    "claims_parameter_supported": false,
                                                    "request_parameter_supported": false,
                                                    "request_uri_parameter_supported": true,
                                                    "token_endpoint_auth_methods_supported": ["client_secret_post","client_secret_basic"]
                                                }
                                                """
                                                .formatted(wireMockServer.baseUrl()))));

        wireMockServer.stubFor(
                get(urlMatching("/auth/slack/openid/connect/authorize.*"))
                        .withQueryParam("scope", equalTo("openid profile email"))
                        .withQueryParam("response_type", equalTo("code"))
                        .withQueryParam("client_id", equalTo("7925551513107.7922794171477"))
                        .withQueryParam("redirect_uri", equalTo("http://localhost:8081/slack"))
                        .withQueryParam("state", new AnythingPattern())
                        .withQueryParam("team", equalTo("quarkus-oidc-slack-demo"))
                        .willReturn(aResponse()
                                .withStatus(302)
                                .withHeader("Content-Type", "text/html")
                                .withHeader("Location", "http://localhost:8081/slack?code=7917304849541.79239831"
                                        + "24323.1f4c41812b286422cbce183a9f083fa58f7c2761c281c2be483a376694f56274&state"
                                        + "={{request.query.state}}")
                                .withBody("")
                                .withTransformers("response-template")));

        wireMockServer.stubFor(
                get(urlMatching("/auth/slack/openid/connect/keys.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                        """
                                                {
                                                  "keys": [
                                                    {
                                                     "kid": "1",
                                                     "kty":"RSA",
                                                     "n":"iJw33l1eVAsGoRlSyo-FCimeOc-AaZbzQ2iESA3Nkuo3TFb1zIkmt0kzlnWVGt48dkaIl13Vdefh9hqw_r9yNF8xZqX1fp0PnCWc5M_TX_ht5fm9y0TpbiVmsjeRMWZn4jr3DsFouxQ9aBXUJiu26V0vd2vrECeeAreFT4mtoHY13D2WVeJvboc5mEJcp50JNhxRCJ5UkY8jR_wfUk2Tzz4-fAj5xQaBccXnqJMu_1C6MjoCEiB7G1d13bVPReIeAGRKVJIF6ogoCN8JbrOhc_48lT4uyjbgnd24beatuKWodmWYhactFobRGYo5551cgMe8BoxpVQ4to30cGA0qjQ",
                                                     "e":"AQAB"
                                                     }
                                                  ]
                                                }
                                                """)));

        wireMockServer.stubFor(
                post(urlMatching("/auth/slack/api/openid\\.connect\\.token.*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                        """
                                                {
                                                  "ok": true,
                                                  "access_token": "xoxp-7925551513107-7925645662178-7911177365927-reduced",
                                                  "token_type": "Bearer",
                                                  "id_token": "%s",
                                                  "state": ""
                                                }
                                                """.formatted(OidcWiremockTestResource.getIdToken("Rosetta",
                                                "7925551513107.7922794171477",
                                                Map.of("name", "Rosetta", "email", "example@example.com"))))
                                .withTransformers("response-template")));
    }

    private static WebClient createWebClient() {
        var webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setRedirectEnabled(true);
        return webClient;
    }
}
