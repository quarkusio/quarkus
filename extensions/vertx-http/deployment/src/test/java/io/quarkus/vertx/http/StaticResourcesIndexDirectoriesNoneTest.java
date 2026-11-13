package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class StaticResourcesIndexDirectoriesNoneTest extends AbstractStaticResourcesIndexDirectoriesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = test("none");

    @Test
    public void directoryWithoutTrailingSlashIsNotFound() {
        given().config(NO_REDIRECTS).get("/classpath").then().statusCode(404);
        given().config(NO_REDIRECTS).get("/generated").then().statusCode(404);
    }
}
