package io.quarkus.oidc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.runtime.OidcClientsConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public abstract class AbstractOidcClientDevServiceTest {

    protected static QuarkusUnitTest createQuarkusUnitTest(String applicationProperties) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addClasses(NamedOidcClientResource.class)
                        .addAsResource(applicationProperties, "application.properties"));
    }

    @Inject
    OidcClientsConfig config;

    @Test
    public void testOidcClientDefaultIdIsSet() {
        var defaultClientConfig = OidcClientsConfig.getDefaultClient(config);
        // not set, so "Default" id should be set by Quarkus
        assertEquals("Default", defaultClientConfig.id().orElse(null));
        // not set, so named key "client1" should be set by Quarkus
        assertEquals("client1", config.namedClients().get("client1").id().orElse(null));
        // set to "client2" in application.properties
        // we cannot set here any different value, because ATM OIDC client enforce that ID always equal named key anyway
        assertEquals("client2", config.namedClients().get("client2").id().orElse(null));
        // not set and named key "client3" is enclosed in double quotes, so named key "client3" should be set by Quarkus
        assertEquals("client3", config.namedClients().get("client3").id().orElse(null));
    }

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
        return OidcCommonUtils.decodeJwtContent(token).getString("upn");
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
