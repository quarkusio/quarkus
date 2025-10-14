package io.quarkus.qute.deployment.devmode;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test that Template#getSource() works correctly in the local dev mode.
 */
public class TemplateSourceDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClasses(SourceRoute.class)
                    .addAsResource(new StringAsset(
                            "{foo}"),
                            "templates/test.html"));

    @Test
    public void testTemplateSource() {
        given().get("test")
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("file:"),
                        Matchers.endsWith("src/main/resources/templates/test.html"));
    }

}
