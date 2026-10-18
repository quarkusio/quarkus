package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static io.restassured.RestAssured.*;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.vertx.test.CookiesSetInFilterTest;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.Headers;

public class MultipleResponseFiltersWithPrioritiesTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CookiesSetInFilterTest.TestResource.class, CookiesSetInFilterTest.Filters.class));

    @Test
    void requestDoesNotContainCookie() {
        when().get("/test")
                .then()
                .statusCode(200)
                .body(is("foo"));
    }

    @Test
    void test() {
        Headers headers = get("/hello")
                .then()
                .statusCode(200)
                .extract().headers();
        assertThat(headers.getValues("filter-response")).containsOnly("max-default-0-minPlus1-min");
    }

    @Path("hello")
    public static class TestResource {

        @GET
        public String get() {
            return "hello";
        }
    }

    @Provider
    @Priority(Integer.MAX_VALUE)
    public static class FilterMax implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().putSingle("filter-response", "max");
        }

    }

    @Provider
    public static class FilterDefault implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            String previousFilterHeaderValue = (String) responseContext.getHeaders().getFirst("filter-response");
            responseContext.getHeaders().putSingle("filter-response", previousFilterHeaderValue + "-default");
        }

    }

    @Provider
    @Priority(0)
    public static class Filter0 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            String previousFilterHeaderValue = (String) responseContext.getHeaders().getFirst("filter-response");
            responseContext.getHeaders().putSingle("filter-response", previousFilterHeaderValue + "-0");
        }

    }

    @Provider
    @Priority(Integer.MIN_VALUE + 1)
    public static class FilterMinPlus1 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            String previousFilterHeaderValue = (String) responseContext.getHeaders().getFirst("filter-response");
            responseContext.getHeaders().putSingle("filter-response", previousFilterHeaderValue + "-minPlus1");
        }

    }

    @Provider
    @Priority(Integer.MIN_VALUE)
    public static class FilterMin implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            String previousFilterHeaderValue = (String) responseContext.getHeaders().getFirst("filter-response");
            responseContext.getHeaders().putSingle("filter-response", previousFilterHeaderValue + "-min");
        }

    }
}
