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
            .withApplicationRoot(root -> root.addClass(NoRestartRoute.class)
                    .addAsResource(new StringAsset("Hello {id}!"), "templates/foo/norestart.html")
                    .addAsResource(new StringAsset("Hi {id}!"), "templates/bar.html")
                    .addAsResource(new StringAsset("quarkus.qute.dev-mode.no-restart-templates=templates/.+"),
                            "application.properties"));

    @Test
    public void testNoRestartTemplates() {
        Response resp = given().get("norestart");
        resp.then().statusCode(200);
        String val1 = resp.getBody().asString();
        assertTrue(val1.startsWith("Hello "));

        resp = given().get("bar");
        resp.then().statusCode(200);
        String val2 = resp.getBody().asString();
        assertTrue(val2.startsWith("Hi "));

        config.modifyResourceFile("templates/foo/norestart.html", t -> t.concat("!!"));
        config.modifyResourceFile("templates/bar.html", t -> t.concat("!!"));

        resp = given().get("norestart");
        resp.then().statusCode(200);
        assertEquals(val1 + "!!", resp.getBody().asString());

        resp = given().get("bar");
        resp.then().statusCode(200);
        assertEquals(val2 + "!!", resp.getBody().asString());
    }

}
