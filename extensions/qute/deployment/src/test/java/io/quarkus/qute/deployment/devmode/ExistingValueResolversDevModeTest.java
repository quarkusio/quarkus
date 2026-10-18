package io.quarkus.qute.deployment.devmode;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test that built-in extension value resolvers are correctly registered after live reload.
 */
public class ExistingValueResolversDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClass(TestRoute.class)
                    .addAsResource(new StringAsset(
                            "{#let a = 3}{#let b = a.minus(2)}b={b}{/}{/}"),
                            "templates/test.html"));

    @Test
    public void testExistingValueResolvers() {
        given().get("test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("b=1"));

        config.modifyResourceFile("templates/test.html", t -> t.concat("::MODIFIED"));

        given().get("test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("b=1::MODIFIED"));
    }

}
