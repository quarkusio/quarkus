package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.test.HelloResource;
import io.quarkus.micrometer.test.MeterResource;
import io.quarkus.test.QuarkusDevModeTest;

public class HttpDevModeConfigTest {
    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest().withApplicationRoot((jar) -> jar
            .addClass(HelloResource.class).addClass(MeterResource.class)
            .add(new StringAsset("quarkus.micrometer.binder-enabled-default=false\n"
                    + "quarkus.micrometer.binder.http-client.enabled=true\n"
                    + "quarkus.micrometer.binder.http-server.enabled=true\n"
                    + "quarkus.micrometer.binder.http-server.ignore-patterns=/http\n"
                    + "quarkus.micrometer.binder.vertx.enabled=true\n" + "quarkus.redis.devservices.enabled=false\n"
                    + "orange=banana"), "application.properties"));

    @Disabled
    @Test
    public void test() throws Exception {

        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().get("/hello/three").then().statusCode(200);
        when().get("/q/dev").then().statusCode(200);
        when().get("/q/metrics").then().statusCode(200).body(containsString("/hello/{message}"));
        when().get("/test/requests").then().statusCode(200).log().body().body(not(containsString("/goodbye/{message}")))
                .body(not(containsString("/q/dev"))) // ignored by default
                .body(not(containsString("/q/metrics"))); // ignored by default

        test.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.micrometer.binder.http-server.ignore-patterns=/http",
                        "quarkus.micrometer.binder.http-server.match-patterns=/hello/.*=/goodbye/{message}"));

        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().get("/hello/three").then().statusCode(200);
        when().get("/q/metrics").then().statusCode(200).body(containsString("/goodbye/{message}"));
        when().get("/test/requests").then().statusCode(200).log().body().body(not(containsString("/hello/{message}")))
                .body(not(containsString("/q/dev"))) // ignored by default
                .body(not(containsString("/q/metrics"))); // ignored by default

        test.modifyResourceFile("application.properties", s -> s.replace("orange=banana",
                "quarkus.micrometer.binder.http-server.suppress-non-application-uris=false"));

        when().get("/hello/three").then().statusCode(200);
        when().get("/q/dev/").then().statusCode(200);
        when().get("/q/metrics").then().statusCode(200).body(containsString("/goodbye/{message}"));
        when().get("/test/requests").then().statusCode(200).log().body().body(not(containsString("/hello/{message}")))
                .body(containsString("/q/dev")).body(containsString("/q/metrics"));

        // Move the non-application endpoint root on top of the http root (both /)
        // Now unable to ignore the non-application root (as it would ignore everything)
        test.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.micrometer.binder.http-server.suppress-non-application-uris=false",
                        "quarkus.http.non-application-root-path=/"));

        when().get("/hello/three").then().statusCode(200);
        when().get("/q/dev").then().statusCode(404);
        when().get("/q/metrics").then().statusCode(404);
        when().get("/dev-ui/favicon.ico").then().statusCode(200);
        when().get("/dev-ui/qwc/qwc-extension.js").then().statusCode(200);
        when().get("/metrics").then().statusCode(200).body(containsString("/goodbye/{message}"))
                .body(containsString("/dev"));
        when().get("/test/requests").then().statusCode(200).log().body().body(containsString("NOT_FOUND"))
                .body(not(containsString("/dev/resources"))) // dev ui requests matched to /dev only
                .body(containsString("/metrics"));

        // Move both http & non-application root to the same something else: /bob
        // Mask for dev UI should be in place, metrics/dev requests should be measured
        test.modifyResourceFile("application.properties", s -> s.replace("quarkus.http.non-application-root-path=/",
                "quarkus.http.non-application-root-path=/bob\nquarkus.http.root-path=/bob"));

        when().get("/bob/hello/three").then().statusCode(200);
        when().get("/bob/dev-ui").then().statusCode(200);
        when().get("/bob/dev-ui/favicon.ico").then().statusCode(200);
        when().get("/bob/dev-ui/qwc/qwc-extension.js").then().statusCode(200);
        when().get("/bob/metrics").then().statusCode(200).body(containsString("/hello/{message}")) // http root prefix
                // is removed in
                // output
                .body(containsString("/bob/dev"));
        when().get("/bob/test/requests").then().statusCode(200).log().body()
                .body(not(containsString("/bob/dev/resources"))) // dev ui requests matched to /dev only
                .body(containsString("/bob/metrics"));

        // Move non-application root to a different something else: /george
        // Able to ignore the non-application root, dev & metrics should be ignored
        test.modifyResourceFile("application.properties", s -> s.replace("quarkus.http.non-application-root-path=/bob",
                "quarkus.http.non-application-root-path=/george"));

        when().get("/bob/hello/three").then().statusCode(200);
        when().get("/george/dev-ui").then().statusCode(200);
        when().get("/george/dev-ui/favicon.ico").then().statusCode(200);
        when().get("/george/metrics").then().statusCode(200).body(containsString("/hello/{message}")); // no longer
                                                                                                       // matches
                                                                                                       // pattern
        when().get("/bob/test/requests").then().statusCode(200).log().body().body(not(containsString("dev"))) // ignored
                .body(not(containsString("metrics"))); // ignored

        // Move non-application root back to something relative to http root
        // Able to ignore the non-application root, dev & metrics should be ignored
        test.modifyResourceFile("application.properties",
                s -> s.replace("quarkus.http.non-application-root-path=/george",
                        "quarkus.http.non-application-root-path=george"));

        when().get("/bob/hello/three").then().statusCode(200);
        when().get("/bob/george/dev-ui").then().statusCode(200);
        when().get("/bob/george/dev-ui/favicon.ico").then().statusCode(200);
        when().get("/bob/george/metrics").then().statusCode(200).body(containsString("/hello/{message}")); // no longer
                                                                                                           // matches
                                                                                                           // pattern,
                                                                                                           // http root
                                                                                                           // removed
        when().get("/bob/test/requests").then().statusCode(200).log().body().body(not(containsString("/george/dev"))) // ignored,
                // http
                // root
                // removed
                .body(not(containsString("/george/metrics"))); // ignored, http root removed
    }

}
