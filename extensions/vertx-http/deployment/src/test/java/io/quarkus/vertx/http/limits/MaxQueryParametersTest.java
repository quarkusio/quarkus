package io.quarkus.vertx.http.limits;

import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class MaxQueryParametersTest {

    private static final String APP_PROPS = """
            quarkus.http.limits.max-query-parameters=2000
            quarkus.http.limits.max-initial-line-length=32768
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @Test
    public void allParametersAreDecodedUpToTheLimit() {
        RestAssured.get("/count?" + query(1500)).then().statusCode(200).body(equalTo("1500"));
    }

    @Test
    public void parametersBeyondTheLimitAreIgnored() {
        RestAssured.get("/count?" + query(2500)).then().statusCode(200).body(equalTo("2000"));
    }

    private static String query(int parameters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters; i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append("p").append(i).append("=v");
        }
        return sb.toString();
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.get("/count").handler(rc -> rc.response().end(String.valueOf(rc.request().params().size())));
        }
    }
}
