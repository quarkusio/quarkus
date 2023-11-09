package io.quarkus.qute.deployment.devmode;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.response.Response;

public class NoRestartTemplatesDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClass(NoRestartRoute.class)
                    .addAsResource(new StringAsset(
                            "Hello {foo}!"),
                            "templates/norestart.html")
                    .addAsResource(new StringAsset(
                            "quarkus.qute.dev-mode.no-restart-templates=templates/norestart.html"),
                            "application.properties"));

    @Test
    public void testNoRestartTemplates() {
        Response resp = given().get("norestart");
        resp.then()
                .statusCode(200);
        String val = resp.getBody().asString();
        assertTrue(val.startsWith("Hello "));

        config.modifyResourceFile("templates/norestart.html", t -> t.concat("!!"));

        resp = given().get("norestart");
        resp.then().statusCode(200);
        assertEquals(val + "!!", resp.getBody().asString());
    }

}
