package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HotReloadWithFilterTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DevBean.class)
                    .addClass(DevFilter.class));

    private static final String USER_FILE = "DevBean.java";
    private static final String USER_FILTER = "DevFilter.java";

    @Test
    public void testFilterChange() {
        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello World"))
                .header("X-Header", is("AAAA"));

        test.modifySourceFile(USER_FILTER, s -> s.replace("AAAA", "BBBB"));

        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello World"))
                .header("X-Header", is("BBBB"));

        test.modifySourceFile(USER_FILE, s -> s.replace("World", "Quarkus"));
        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello Quarkus"))
                .header("X-Header", is("BBBB"));

        test.modifySourceFile(USER_FILTER, s -> s.replace("BBBB", "CCC"));

        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello Quarkus"))
                .header("X-Header", is("CCC"));

    }

    @Test
    public void testAddFilter() {
        test.addSourceFile(NewFilter.class);

        RestAssured.when().get("/dev").then()
                .statusCode(200)
                .body(is("Hello World"))
                .header("X-Header", is("AAAA"))
                .header("X-Header-2", is("Some new header"));
    }
}
