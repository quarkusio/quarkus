package io.quarkus.it.extension;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.extest.runtime.TestHotReplacementSetup;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HotReplacementSetupDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SystemPropertyTestEndpoint.class)
                    .addAsResource(new StringAsset("nothing"), TestHotReplacementSetup.HOT_REPLACEMENT_FILE));

    @Test
    public void watched() {
        RestAssured.get("/core/sysprop")
                .then()
                .statusCode(200);
        TEST.modifyResourceFile(TestHotReplacementSetup.HOT_REPLACEMENT_FILE, text -> "throw");
        RestAssured.get("/core/sysprop")
                .then()
                .statusCode(500)
                .body(Matchers.containsString("Generated page for exception"));
        TEST.modifyResourceFile(TestHotReplacementSetup.HOT_REPLACEMENT_FILE, text -> "nothing");
        RestAssured.get("/core/sysprop")
                .then()
                .statusCode(200);
    }
}
