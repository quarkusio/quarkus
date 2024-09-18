package io.quarkus.oidc.client.reactive.filter;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class NamedOidcClientFilterDevModeTest {

    private static final Class<?>[] testClasses = {
            ProtectedResource.class,
            ProtectedResourceServiceAnnotationOidcClient.class,
            ProtectedResourceServiceConfigPropertyOidcClient.class,
            ProtectedResourceServiceCustomProviderConfigPropOidcClient.class,
            OidcClientResource.class,
            ClientWebApplicationExceptionMapper.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-reactive-filter.properties", "application.properties"));

    @Test
    public void testGerUserConfigPropertyAndAnnotation() {
        // test OidcClientFilter with OidcClient selected via annotation or config-property

        // Client feature is disabled
        // OidcClient selected via @OidcClient("clientName")
        RestAssured.when().get("/oidc-client/annotation/user-name")
                .then()
                .statusCode(401);

        RestAssured.when().get("/oidc-client/annotation/anonymous-user-name")
                .then()
                .statusCode(204)
                .body(equalTo(""));

        // @OidcClientFilter: OidcClient selected via `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/oidc-client/config-property/user-name")
                .then()
                .statusCode(401);

        RestAssured.when().get("/oidc-client/config-property/anonymous-user-name")
                .then()
                .statusCode(204)
                .body(equalTo(""));

        // @RegisterProvider(OidcClientRequestReactiveFilter.class): OidcClient selected via `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/oidc-client/custom-provider-config-property/user-name")
                .then()
                .statusCode(401);

        RestAssured.when().get("/oidc-client/custom-provider-config-property/anonymous-user-name")
                .then()
                .statusCode(204)
                .body(equalTo(""));

        test.modifyResourceFile("application.properties", s -> s.replace(".enabled=false", ".enabled=true"));

        // Client feature is enabled
        // OidcClient selected via @OidcClient("clientName")
        RestAssured.when().get("/oidc-client/annotation/user-name")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));

        RestAssured.when().get("/oidc-client/annotation/anonymous-user-name")
                .then()
                .statusCode(200)
                .body(equalTo("jdoe"));

        // @OidcClientFilter: OidcClient selected via `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/oidc-client/config-property/user-name")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

        RestAssured.when().get("/oidc-client/config-property/anonymous-user-name")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));

        // @RegisterProvider(OidcClientRequestReactiveFilter.class): OidcClient selected via `quarkus.oidc-client-filter.client-name=config-property`
        RestAssured.when().get("/oidc-client/custom-provider-config-property/user-name")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
        RestAssured.when().get("/oidc-client/custom-provider-config-property/anonymous-user-name")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

}
