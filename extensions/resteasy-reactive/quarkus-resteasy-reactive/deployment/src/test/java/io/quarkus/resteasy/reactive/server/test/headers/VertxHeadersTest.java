package io.quarkus.resteasy.reactive.server.test.headers;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

public class VertxHeadersTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(VertxFilter.class, JaxRsFilter.class, TestResource.class));

    @Test
    void testVaryHeaderValues() {
        var headers = when().get("/test")
                .then()
                .statusCode(200)
                .extract().headers();
        assertThat(headers.getValues(HttpHeaders.VARY)).containsExactlyInAnyOrder("Origin", "Prefer");
    }

    public static class VertxFilter {
        @RouteFilter
        void addVary(final RoutingContext rc) {
            rc.response().headers().add(HttpHeaders.VARY, "Origin");
            rc.next();
        }
    }

    @Provider
    public static class JaxRsFilter implements ContainerResponseFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add(HttpHeaders.VARY, "Prefer");
        }
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String test() {
            return "test";
        }
    }
}
