package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RootPathTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/api\n";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @Test
    public void test() throws Exception {
        // RestAssured is aware of quarkus.http.root-path
        // If this changes then please modify quarkus-azure-functions-http maven archetype to reflect this
        // in its test classes
        RestAssured.given().get("/observes").then().statusCode(200).body(Matchers.equalTo("/api/observes"));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.route("/observes").handler(rc -> {
                String path = rc.request().path();
                rc.response().end(path);
            });
        }

    }

}
