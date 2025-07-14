package io.quarkus.vertx.http.devmode;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Tests that once a file has a compile error restart will not happen until it is fixed, even if
 * other files are subsequently modified that do compile.
 */
public class CompileProblemTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CompileErrorEndpoint.class, CompileCorrectlyEndpoint.class));

    @Test
    public void test() {
        RestAssured.get("/error").then().body(equalTo("error"));
        RestAssured.get("/correct").then().body(equalTo("correct"));
        test.modifySourceFile(CompileErrorEndpoint.class, s -> s.replace("\"error\"", "\"compile error"));
        RestAssured.get("/error").then().statusCode(500);
        RestAssured.get("/correct").then().statusCode(500);
        test.modifySourceFile(CompileCorrectlyEndpoint.class, s -> s.replace("\"correct\"", "\"compiled correctly\""));
        //make sure that we are still in an error state, as CompileErrorEndpoint is broken
        RestAssured.get("/error").then().statusCode(500);
        RestAssured.get("/correct").then().statusCode(500);
        test.modifySourceFile(CompileErrorEndpoint.class, s -> s.replace("compile error", "compile error fixed\""));
        RestAssured.get("/error").then().body(equalTo("compile error fixed"));
        RestAssured.get("/correct").then().body(equalTo("compiled correctly"));
    }
}
