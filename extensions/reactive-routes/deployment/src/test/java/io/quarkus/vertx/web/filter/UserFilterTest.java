package io.quarkus.vertx.web.filter;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;

import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class UserFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyFilters.class));

    @Test
    public void test() {
        get("/").then().statusCode(200)
                .header("X-Filter1", not(nullValue()))
                .header("X-Filter2", not(nullValue()))
                // filter 1 is called after filter 2 so override the value
                .header("X-Filter", "filter 1")
                .body(is("OK"));
    }

    public static class MyFilters {

        @RouteFilter // the default priority is 10
        void filter1(RoutingContext rc) {
            rc.response().putHeader("X-Filter1", Long.toString(System.nanoTime()));
            rc.response().putHeader("X-Filter", "filter 1");
            rc.next();
        }

        @RouteFilter(20)
        void filter2(RoutingContext rc) {
            rc.response().putHeader("X-Filter2", Long.toString(System.nanoTime()));
            rc.response().putHeader("X-Filter", "filter 2");
            rc.next();
        }

        void register(@Observes Router router) {
            router
                    .get("/")
                    .handler(rc -> rc.response().end("OK"));
        }

    }

}
