package io.quarkus.vertx.web.filter;

import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.restassured.RestAssured;
import io.vertx.ext.web.RoutingContext;

public class UserFilterRequestContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FilterAndRoute.class, RequestFoo.class));

    @Test
    public void test() {
        RestAssured.post("/hello").then().statusCode(200)
                .body(is("11"));
    }

    public static class FilterAndRoute {

        @Inject
        RequestFoo foo;

        @RouteFilter
        void filter1(RoutingContext rc) {
            foo.setState(11);
            rc.next();
        }

        @Route(path = "hello")
        void hello(RoutingContext ctx) {
            ctx.response().end("" + foo.getState());
        }

    }

    @RequestScoped
    static class RequestFoo {

        private AtomicInteger state = new AtomicInteger(-1);

        void setState(int value) {
            this.state.set(value);
        }

        public int getState() {
            return state.get();
        }

    }

}
