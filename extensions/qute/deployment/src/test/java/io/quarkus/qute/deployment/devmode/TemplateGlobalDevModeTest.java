package io.quarkus.qute.deployment.devmode;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateGlobal;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Test that template globals added in the dev mode are generated correctly after live reload.
 * <p>
 * The {@link QuteDummyTemplateGlobalMarker} is used to identify an application archive where a dummy built-in class with
 * template globals is added.
 */
public class TemplateGlobalDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClasses(TestRoute.class, MyGlobals.class, QuteDummyTemplateGlobalMarker.class)
                    .addAsResource(new StringAsset(
                            "{foo}:{quteDummyFoo}:{testFoo ?: 'NA'}"),
                            "templates/test.html"));

    @Test
    public void testTemplateGlobals() {
        given().get("test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("24:bar:NA"));

        // Add application globals - the priority sequence should be automatically
        // increased before it's used for TestGlobals
        config.addSourceFile(TestGlobals.class);

        given().get("test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("24:bar:baz"));
    }

    @TemplateGlobal
    public static class MyGlobals {

        public static int foo = 24;

    }

}
