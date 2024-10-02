package io.quarkus.oidc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test Keycloak Dev Service is started when OIDC extension is disabled (or not present, though indirectly).
 * OIDC client auth server URL and client id and secret must be automatically configured for this test to pass.
 */
public class OidcClientKeycloakDevServiceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NamedOidcClientResource.class)
                    .addAsResource("oidc-client-dev-service-test.properties", "application.properties"));

    @Test
    public void testInjectedNamedOidcClients() {
        String token1 = doTestGetTokenByNamedClient("client1");
        String token2 = doTestGetTokenByNamedClient("client2");
        validateTokens(token1, token2);
    }

    @Test
    public void testInjectedNamedTokens() {
        String token1 = doTestGetTokenByNamedTokensProvider("client1");
        String token2 = doTestGetTokenByNamedTokensProvider("client2");
        validateTokens(token1, token2);
    }

    private void validateTokens(String token1, String token2) {
        assertThat(token1, is(not(equalTo(token2))));
        assertThat(upn(token1), is("alice"));
        assertThat(upn(token2), is("bob"));
    }

    private String upn(String token) {
        return OidcUtils.decodeJwtContent(token).getString("upn");
    }

    private String doTestGetTokenByNamedClient(String clientId) {
        String token = RestAssured.given().get("/" + clientId + "/token").body().asString();
        assertThat(token, is(notNullValue()));
        return token;
    }

    private String doTestGetTokenByNamedTokensProvider(String clientId) {
        String token = RestAssured.given().get("/" + clientId + "/token/singleton").body().asString();
        assertThat(token, is(notNullValue()));
        return token;
    }
}
