package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * The configured minimum remaining lifespan is carried by the acquired tokens, which is what enables their reuse while
 * they are being refreshed.
 */
@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
public class OidcClientMinRemainingAccessTokenLifespanTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "# Disable Dev Services, Keycloak is started by a Maven plugin\n"
                                    + "quarkus.keycloak.devservices.enabled=false\n"
                                    + "quarkus.oidc.tenant-enabled=false\n"
                                    + "quarkus.oidc-client.auth-server-url=${keycloak.url}/realms/quarkus/\n"
                                    + "quarkus.oidc-client.client-id=quarkus-app\n"
                                    + "quarkus.oidc-client.credentials.secret=secret\n"
                                    + "quarkus.oidc-client.grant.type=password\n"
                                    + "quarkus.oidc-client.grant-options.password.username=alice\n"
                                    + "quarkus.oidc-client.grant-options.password.password=alice\n"
                                    + "quarkus.oidc-client.refresh-token-time-skew=2S\n"
                                    + "quarkus.oidc-client.min-remaining-access-token-lifespan=1S\n"),
                            "application.properties"));

    @Inject
    OidcClient client;

    @Test
    public void testTokensCarryMinRemainingAccessTokenLifespan() {
        Tokens tokens = client.getTokens().await().indefinitely();

        assertEquals(1L, tokens.getMinRemainingAccessTokenLifespan());
        assertEquals(2L, tokens.getRefreshTokenTimeSkew());
    }
}
