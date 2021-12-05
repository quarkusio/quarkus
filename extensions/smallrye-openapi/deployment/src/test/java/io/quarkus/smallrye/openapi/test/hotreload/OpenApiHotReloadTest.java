package io.quarkus.smallrye.openapi.test.hotreload;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenApiHotReloadTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyResource.class));

    @Test
    public void testAddingAndDeletingEndpoint() {
        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"));

        TEST.modifySourceFile("MyResource.java", s -> s.replace("// <placeholder>",
                "@GET @Path(\"foo\")" // Important - must be on the same line
                        + "public String greeting() { "
                        + " return \"bonjour\"; "
                        + "}"));

        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"))
                .body(containsString("/api/foo"));

        TEST.modifySourceFile("MyResource.java", s -> s.replace("foo", "bar"));

        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"))
                .body(not(containsString("/api/foo")))
                .body(containsString("/api/bar"));

        TEST.modifySourceFile("MyResource.java", s -> s.replace("@GET @Path(\"bar\")", ""));

        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"))
                .body(not(containsString("/api/foo")))
                .body(not(containsString("/api/bar")));
    }

    @Test
    public void testAddingAndUpdatingResource() {
        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"));

        TEST.addSourceFile(MySecondResource.class);

        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"))
                .body(containsString("/my-second-api"));

        TEST.modifySourceFile("MySecondResource.java", s -> s.replace("my-second-api", "/foo"));

        RestAssured.get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("/api"))
                .body(not(containsString("/my-second-api")))
                .body(containsString("/foo"));
    }

}
