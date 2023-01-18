package io.quarkus.test.qute;

import static org.hamcrest.CoreMatchers.containsString;

import jakarta.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class QuteErrorPageTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    root -> root.addAsResource(new StringAsset("{hello.foo}"), "templates/hello.txt"));

    @Test
    public void testErrorPage() {
        config.modifyResourceFile("templates/hello.txt", file -> "{@java.lang.String hello}{hello.foo}");
        RestAssured.when().get("/hello").then()
                .body(containsString("hello.txt:1"), containsString("{hello.foo}"))
                .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
