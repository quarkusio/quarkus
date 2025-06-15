package io.quarkus.vertx.http;

import static org.hamcrest.Matchers.nullValue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class StaticResourcesCachingDisabledTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .add(new StringAsset("quarkus.http.static-resources.caching-enabled=false\n"), "application.properties")
            .addAsResource("static-file.html", "META-INF/resources/index.html"));

    @Test
    public void shouldNotContainCachingHeaders() {
        RestAssured.when().get("/").then().header("Cache-Control", nullValue()).header("Last-Modified", nullValue())
                .statusCode(200);
    }

}
