package io.quarkus.vertx.http.filters;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.ext.web.Router;

public class UserFilterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, UserFilterTest.class));

    @Test
    public void test() {
        get("/").then().statusCode(200)
                .header("X-Filter1", not(nullValue()))
                .header("X-Filter2", not(nullValue()))
                // filter 1 is called after filter 2 so override the value
                .header("X-Filter", "filter 1")
                .body(is("OK"));
    }

    @ApplicationScoped
    public static class MyBean {

        public void filters(@Observes Filters filters) {
            filters
                    .register(
                            rc -> {
                                rc.response().putHeader("X-Filter1", Long.toString(System.nanoTime()));
                                rc.response().putHeader("X-Filter", "filter 1");
                                rc.next();
                            },
                            10)
                    .register(
                            rc -> {
                                rc.response().putHeader("X-Filter2", Long.toString(System.nanoTime()));
                                rc.response().putHeader("X-Filter", "filter 2");
                                rc.next();
                            },
                            20);

        }

        public void register(@Observes Router router) {
            router
                    .get("/")
                    .handler(rc -> rc.response().end("OK"));
        }

    }

}
