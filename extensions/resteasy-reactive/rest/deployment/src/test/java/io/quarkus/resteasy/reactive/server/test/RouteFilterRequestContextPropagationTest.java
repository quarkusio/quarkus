package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.ext.web.RoutingContext;

public class RouteFilterRequestContextPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(RequestFoo.class, MyFilter.class, MyResource.class));

    @Test
    void testBlockingEndpoint() {
        when().get("/hello/blocking")
                .then()
                .statusCode(200)
                .body(is("route-filter-value"));
    }

    @Test
    void testNonBlockingEndpoint() {
        when().get("/hello/non-blocking")
                .then()
                .statusCode(200)
                .body(is("route-filter-value"));
    }

    @RequestScoped
    public static class RequestFoo {

        private volatile String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class MyFilter {

        @Inject
        RequestFoo foo;

        @RouteFilter
        void filter(RoutingContext rc) {
            foo.setValue("route-filter-value");
            rc.next();
        }
    }

    @Path("hello")
    public static class MyResource {

        @Inject
        RequestFoo foo;

        @GET
        @Path("blocking")
        public String blocking() {
            return foo.getValue();
        }

        @GET
        @Path("non-blocking")
        @NonBlocking
        public String nonBlocking() {
            return foo.getValue();
        }
    }
}
