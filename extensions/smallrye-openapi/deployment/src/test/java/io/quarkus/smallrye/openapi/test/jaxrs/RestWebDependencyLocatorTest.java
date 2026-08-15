package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class RestWebDependencyLocatorTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RestJsClientTest.GreetingResource.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"))
            .overrideConfigKey("quarkus.smallrye-openapi.js-client.enabled", "true");

    @Test
    public void testImportMapContainsRestMappings() {
        RestAssured.get("/_importmap/generated_importmap.js")
                .then()
                .statusCode(200)
                .body(containsString("@quarkus/rest"))
                .body(containsString("/_static/quarkus-rest/rest-client.js"))
                .body(containsString("@quarkus/rest-api"))
                .body(containsString("/_static/quarkus-rest-api/rest-api.js"));
    }
}
