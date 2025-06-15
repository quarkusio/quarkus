package io.quarkus.vertx.http;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class StaticResourcesCustomizedPagesTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .add(new StringAsset("" + "quarkus.http.static-resources.index-page=default.html\n"
                    + "quarkus.http.static-resources.include-hidden=false\n"
                    + "quarkus.http.static-resources.enable-range-support=false\n"), "application.properties")
            .addAsResource("static-file.html", "META-INF/resources/.hidden-file.html")
            .addAsResource("static-file.html", "META-INF/resources/default.html"));

    @Test
    public void shouldContainCachingHeaders() {
        RestAssured.when().get("/").then().header("Cache-Control", containsStringIgnoringCase("max-age="))
                .header("Last-Modified", notNullValue()).statusCode(200);
    }

    @Test
    public void shouldNotReturnHiddenHtmlPage() {
        RestAssured.when().get("/.hidden-file.html").then().statusCode(404);
    }

    @Test
    public void shouldNotReturnRangeSupport() {
        RestAssured.when().head("/").then().header("Accept-Ranges", nullValue()).header("Content-Length", nullValue())
                .statusCode(200);
    }

}
