package io.quarkus.vertx.web.failure;

import static io.quarkus.vertx.web.Route.HandlerType.FAILURE;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;

public class FailureHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Routes.class));

    @Test
    public void test() {
        get("/fail?type=unsupported").then().statusCode(501).body(is("Unsupported!"));
        get("/fail").then().statusCode(500).body(is("Unknown!"));
    }

    public static class Routes {

        @Route
        String fail(@Param String type) {
            if ("unsupported".equals(type)) {
                throw new UnsupportedOperationException("Unsupported!");
            } else {
                throw new RuntimeException("Unknown!");
            }
        }

        @Route(path = "/fail", type = FAILURE, order = 1)
        void uoe(UnsupportedOperationException e, HttpServerResponse response) {
            response.setStatusCode(501).end(e.getMessage());
        }

        @Route(type = FAILURE, order = 2)
        void re(RuntimeException e, HttpServerResponse response) {
            response.setStatusCode(500).end(e.getMessage());
        }

    }

}
