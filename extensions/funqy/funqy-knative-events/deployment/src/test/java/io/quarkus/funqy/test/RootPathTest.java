package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RootPathTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/api\n" +
            "quarkus.funqy.export=toLowerCase\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(PrimitiveFunctions.class));

    @Test
    public void testRoot() {
        // RestAssured is aware of quarkus.http.root-path
        RestAssured.given().contentType("application/json").body("\"Hello Test\"").post("/")
                .then().statusCode(200).body(equalTo("\"hello test\""));
        RestAssured.given().contentType("application/json").body("\"Hello Test\"").post("/toLowerCase")
                .then().statusCode(200).body(equalTo("\"hello test\""));
    }
}
