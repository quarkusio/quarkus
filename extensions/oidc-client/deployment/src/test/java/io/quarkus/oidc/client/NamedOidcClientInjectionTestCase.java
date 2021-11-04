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
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
public class NamedOidcClientInjectionTestCase {

    private static Class<?>[] testClasses = {
            NamedOidcClientResource.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-named-oidc-client-credentials.properties", "application.properties"));

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
        assertThat(preferredUserOf(token1), is("alice"));
        assertThat(preferredUserOf(token2), is("bob"));
    }

    private String preferredUserOf(String token) {
        return OidcUtils.decodeJwtContent(token).getString("preferred_username");
    }

    private String doTestGetTokenByNamedClient(String clientId) {
        String token = RestAssured.when().get("/" + clientId + "/token").body().asString();
        assertThat(token, is(notNullValue()));
        return token;
    }

    private String doTestGetTokenByNamedTokensProvider(String clientId) {
        String token = RestAssured.when().get("/" + clientId + "/token/singleton").body().asString();
        assertThat(token, is(notNullValue()));
        return token;
    }
}
