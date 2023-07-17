package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RootPathTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/api\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(PrimitiveFunctions.class, GreetingFunctions.class, Greeting.class, GreetingService.class,
                            GreetingTemplate.class));

    @Test
    public void testGetOrPost() throws Exception {
        // RestAssured is aware of quarkus.http.root-path
        RestAssured.given().get("/get")
                .then().statusCode(200).body(equalTo("\"get\""));
        RestAssured.given().post("/get")
                .then().statusCode(200).body(equalTo("\"get\""));
    }

}
