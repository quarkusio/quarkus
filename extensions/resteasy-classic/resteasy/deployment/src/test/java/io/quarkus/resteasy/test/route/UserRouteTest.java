package io.quarkus.resteasy.test.route;

import static org.hamcrest.core.Is.is;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.test.RootResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

/**
 * Test that user route are still called even if registered after the default route.
 */
public class UserRouteTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootResource.class, MyRoute.class));

    @Inject
    MyRoute myRoute;

    @Test
    public void test() {
        // test accessing the root resource
        RestAssured.get("/").then()
                .statusCode(200)
                .body(is("Root Resource"));
        // test without the route
        RestAssured.get("/my-route").then()
                .statusCode(404);

        myRoute.register();

        // test with the route
        RestAssured.get("/my-route").then()
                .statusCode(200)
                .body(is("OK"));

        // test we can still access the default route
        RestAssured.get("/").then()
                .statusCode(200)
                .body(is("Root Resource"));
    }

    @ApplicationScoped
    public static class MyRoute {

        @Inject
        Router router;

        public void register() {
            router.route("/my-route").handler(rc -> rc.response().end("OK"));
        }

    }

}
