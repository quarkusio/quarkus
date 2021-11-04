package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.core.Is.is;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class HotReloadWithRouteTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(DevBean.class));
    private static final String USER_FILE = "DevBean.java";

    @TestHTTPResource
    URL url;

    @Test
    public void testRouteChange() {
        RestAssured.when().get("http://localhost:" + url.getPort() + "/dev").then()
                .statusCode(200)
                .body(is("Hello World"));

        test.modifySourceFile(USER_FILE, s -> s.replace("Hello World", "Hello Quarkus"));

        RestAssured.when().get("http://localhost:" + url.getPort() + "/dev").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));

        test.modifySourceFile("DevBean.java", s -> s.replace("/dev", "/new"));

        RestAssured.when().get("http://localhost:" + url.getPort() + "/dev").then()
                .statusCode(404);

        RestAssured.when().get("http://localhost:" + url.getPort() + "/new").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));
    }

    @Test
    public void testAddBean() {
        RestAssured.when().get("http://localhost:" + url.getPort() + "/bean").then()
                .statusCode(404);

        test.addSourceFile(NewBean.class);

        RestAssured.when().get("http://localhost:" + url.getPort() + "/bean").then()
                .statusCode(200)
                .body(is("Hello New World"));
    }
}
