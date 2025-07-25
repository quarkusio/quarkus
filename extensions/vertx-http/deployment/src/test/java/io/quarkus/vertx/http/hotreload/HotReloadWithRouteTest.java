package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HotReloadWithRouteTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(DevBean.class));
    private static final String USER_FILE = "DevBean.java";

    @Test
    public void testRouteChange() {
        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello World"));

        test.modifySourceFile(USER_FILE, s -> s.replace("Hello World", "Hello Quarkus"));

        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));

        test.modifySourceFile("DevBean.java", s -> s.replace("/dev", "/new"));

        RestAssured.when().get("/dev").then()
                .statusCode(404);

        RestAssured.when().get("/new").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));
    }

    @Test
    public void testAddBean() {
        RestAssured.when().get("/bean").then()
                .statusCode(404);

        test.addSourceFile(NewBean.class);

        RestAssured.when().get("/bean").then()
                .statusCode(200)
                .body(is("Hello New World"));
    }
}
