package io.quarkus.resteasy.test.files;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class StaticResourceDeletionBeforeFirstRequestTest {

    public static final String META_INF_RESOURCES_STATIC_RESOURCE_TXT = "META-INF/resources/static-resource.txt";

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("static resource content"), META_INF_RESOURCES_STATIC_RESOURCE_TXT));

    @Test
    public void shouldReturn404HttpStatusCode() {
        test.deleteResourceFile(META_INF_RESOURCES_STATIC_RESOURCE_TXT); // delete the resource
        RestAssured.when().get("/static-resource.txt").then().statusCode(404);
    }
}
