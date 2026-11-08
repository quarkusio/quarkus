package io.quarkus.vertx.http.devmode;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class StaticResourcesDeletionDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("a"), "META-INF/resources/a.txt")
                    .addAsResource(new StringAsset("index"), "META-INF/resources/index.html"));

    @Test
    void deletedFileIsNoLongerServed() {
        RestAssured.when().get("/a.txt").then().statusCode(200).body(is("a"));
        test.addResourceFile("META-INF/resources/b.txt", "b");
        RestAssured.when().get("/b.txt").then().statusCode(200).body(is("b"));
        test.deleteResourceFile("META-INF/resources/a.txt");
        RestAssured.when().get("/a.txt").then().statusCode(404);
        RestAssured.when().get("/b.txt").then().statusCode(200).body(is("b"));
    }

    @Test
    void deletedIndexPageIsNoLongerServed() {
        RestAssured.when().get("/").then().statusCode(200).body(is("index"));
        test.deleteResourceFile("META-INF/resources/index.html");
        RestAssured.when().get("/").then().statusCode(404);
    }
}
