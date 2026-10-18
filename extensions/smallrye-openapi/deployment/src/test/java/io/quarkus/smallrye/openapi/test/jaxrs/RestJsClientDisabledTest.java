package io.quarkus.smallrye.openapi.test.jaxrs;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class RestJsClientDisabledTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RestJsClientTest.GreetingResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testClientLibraryNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-rest/rest-client.js")
                .then()
                .statusCode(404);
    }

    @Test
    public void testTypedProxyNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.js")
                .then()
                .statusCode(404);
    }

    @Test
    public void testClientDeclarationsNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-rest/rest-client.d.ts")
                .then()
                .statusCode(404);
    }

    @Test
    public void testProxyDeclarationsNotServedWhenDisabled() {
        RestAssured.get("/_static/quarkus-rest-api/rest-api.d.ts")
                .then()
                .statusCode(404);
    }
}
