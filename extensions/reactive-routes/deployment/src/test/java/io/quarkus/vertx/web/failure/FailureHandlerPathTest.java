package io.quarkus.vertx.web.failure;

import static io.quarkus.vertx.web.Route.HandlerType.FAILURE;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;

public class FailureHandlerPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class));

    @Test
    public void test() {
        get("/fail").then().statusCode(500).body(is("no-path"));
    }

    public static class Routes {

        @Route
        String fail(@Param String type) {
            throw new RuntimeException("Unknown!");
        }

        // this path should not match
        @Route(path = "/liaf", type = FAILURE, order = 1)
        void onFailure(HttpServerResponse response) {
            response.setStatusCode(501).end("liaf");
        }

        @Route(type = FAILURE, order = 2)
        void onFailureNoPath(HttpServerResponse response) {
            response.setStatusCode(500).end("no-path");
        }

    }

}
