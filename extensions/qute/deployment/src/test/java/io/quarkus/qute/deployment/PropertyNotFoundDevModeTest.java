package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;

public class PropertyNotFoundDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest testConfig = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Routes.class)
                    .addAsResource(new StringAsset("{foo.surname}"), "templates/foo.html")
                    .addAsResource(new StringAsset("{bar.name}"), "templates/bar.html"));

    @Test
    public void testExceptionIsThrown() {
        assertEquals(
                "Rendering error in template [foo.html] line 1: Entry \"foo\" not found in the data map in expression {foo.surname}",
                RestAssured.get("test-foo").then().statusCode(200).extract().body().asString());
        assertEquals(
                "Rendering error in template [bar.html] line 1: Property \"name\" not found on the base object \"java.lang.String\" in expression {bar.name}",
                RestAssured.get("test-bar").then().statusCode(200).extract().body().asString());
    }

    @Singleton
    public static class Routes {

        @Inject
        Template foo;

        @Route(produces = "text/plain")
        String testFoo() {
            try {
                return foo.render();
            } catch (TemplateException e) {
                return e.getMessage();
            }
        }

        @Inject
        Template bar;

        @Route(produces = "text/plain")
        String testBar() {
            try {
                return bar.data("bar", "alpha").render();
            } catch (TemplateException e) {
                return e.getMessage();
            }
        }

    }

}
