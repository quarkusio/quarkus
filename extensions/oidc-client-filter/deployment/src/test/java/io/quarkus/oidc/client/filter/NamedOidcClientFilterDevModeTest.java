package io.quarkus.oidc.client.filter;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class NamedOidcClientFilterDevModeTest {

    private static final Class<?>[] testClasses = { ProtectedResource.class,
            ProtectedResourceServiceNamedOidcClient.class, ProtectedResourceServiceConfigPropertyOidcClient.class,
            ProtectedResourceServiceCustomProviderConfigPropOidcClient.class, NamedOidcClientResource.class,
            ConfigPropertyOidcClientResource.class };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(testClasses)
                    .addAsResource("application-named-oidc-client-filter.properties", "application.properties"));

    @Test
    public void testGerUserConfigPropertyAndAnnotation() {
        // OidcClient selected via @OidcClient("clientName")
        RestAssured.when().get("/named-oidc-client/user-name").then().statusCode(200).body(equalTo("jdoe"));

        // @OidcClientFilter: OidcClient selected via `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/config-property-oidc-client/annotation/user-name").then().statusCode(200)
                .body(equalTo("alice"));

        // @RegisterProvider(OidcClientRequestFilter.class): OidcClient selected via
        // `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/config-property-oidc-client/custom-provider/user-name").then().statusCode(200)
                .body(equalTo("alice"));
    }

}
