package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class StaticResourcesIndexDirectoriesRerouteTest extends AbstractStaticResourcesIndexDirectoriesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = test("reroute");

    @Test
    public void directoryWithoutTrailingSlashServesTheIndexPage() {
        given().config(NO_REDIRECTS).get("/classpath").then().statusCode(200).body(equalTo(CLASSPATH_INDEX));
        given().config(NO_REDIRECTS).get("/generated").then().statusCode(200).body(equalTo(GENERATED_INDEX));
    }
}
