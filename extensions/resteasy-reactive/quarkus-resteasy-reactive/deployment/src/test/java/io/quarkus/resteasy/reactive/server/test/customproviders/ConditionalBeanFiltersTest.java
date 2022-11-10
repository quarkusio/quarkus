package io.quarkus.resteasy.reactive.server.test.customproviders;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.Header;
import io.smallrye.mutiny.Uni;

public class ConditionalBeanFiltersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(WontBeEnabledFilter.class, WillBeEnabledFilter.class, AlwaysEnabledFilter.class,
                                    TestResource.class);
                }
            });

    @Test
    public void testExpectedFilters() {
        List<String> responseFiltersValues = get("/test/filters")
                .then().statusCode(200)
                .body(Matchers.is("void-on,response-on,uni-on,always"))
                .extract()
                .headers()
                .getList("response-filters")
                .stream()
                .map(Header::getValue)
                .collect(Collectors.toList());
        assertThat(responseFiltersValues).containsOnly("always", "void-on", "uni-on");
    }

    @Path("test")
    public static class TestResource {

        @Path("filters")
        @GET
        public String filters(HttpHeaders headers) {
            return String.join(",", headers.getRequestHeader("request-filters"));
        }
    }

    @IfBuildProperty(name = "notexistingproperty", stringValue = "true")
    public static class WontBeEnabledFilter {

        @ServerRequestFilter(priority = Priorities.USER + 1)
        public void voidRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "void-off");
        }

        @ServerRequestFilter(priority = Priorities.USER + 2)
        public Response responseTypeRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "response-off");
            return null;
        }

        @ServerRequestFilter(priority = Priorities.USER + 3)
        public Uni<Void> uniRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "uni-off");
            return Uni.createFrom().nullItem();
        }

        // if any of the following were to be executed, they would fail, thrown an exception and result in an HTTP 500

        @ServerResponseFilter
        public void voidResponseFilter(ContainerResponseContext ctx) {
            assertFalse(true);
        }

        @ServerResponseFilter
        public Uni<Void> uniResponseFilter(ContainerResponseContext ctx) {
            assertFalse(true);
            return Uni.createFrom().nullItem();
        }
    }

    @IfBuildProfile("test")
    public static class WillBeEnabledFilter {

        @ServerRequestFilter(priority = Priorities.USER + 4)
        public void voidRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "void-on");
        }

        @ServerRequestFilter(priority = Priorities.USER + 5)
        public Optional<Response> responseTypeRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "response-on");
            return Optional.empty();
        }

        @ServerRequestFilter(priority = Priorities.USER + 6)
        public Uni<Response> uniRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "uni-on");
            return Uni.createFrom().nullItem();
        }

        @ServerResponseFilter(priority = Priorities.USER + 4)
        public void voidResponseFilter(ContainerResponseContext ctx) {
            ctx.getHeaders().add("response-filters", "void-on");
        }

        @ServerResponseFilter(priority = Priorities.USER + 6)
        public Uni<Void> uniResponseFilter(ContainerResponseContext ctx) {
            ctx.getHeaders().add("response-filters", "uni-on");
            return Uni.createFrom().nullItem();
        }
    }

    @Singleton
    public static class AlwaysEnabledFilter {

        @ServerRequestFilter(priority = Priorities.USER + 100)
        public void alwaysRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "always");
        }

        @ServerResponseFilter(priority = Priorities.USER + 100)
        public void voidResponseFilter(ContainerResponseContext ctx) {
            ctx.getHeaders().add("response-filters", "always");
        }
    }
}
