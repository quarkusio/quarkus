package io.quarkus.resteasy.reactive.server.test.customproviders;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.arc.lookup.LookupUnlessProperty;
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
                .body(Matchers.is("void-on,response-on,uni-on,void-lookup-on,always"))
                .extract()
                .headers()
                .getList("response-filters")
                .stream()
                .map(Header::getValue)
                .collect(Collectors.toList());
        assertThat(responseFiltersValues).containsOnly("always", "void-lookup-on", "void-on", "uni-on");
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

    @LookupIfProperty(name = "notexistingproperty", stringValue = "true")
    public static class WontBeEnabledLookupPropertyFilter {

        @ServerRequestFilter(priority = Priorities.USER + 10)
        public void voidRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "void-lookup-off");
        }

        @ServerResponseFilter
        public void voidResponseFilter(ContainerResponseContext ctx) {
            assertFalse(true);
        }
    }

    @LookupUnlessProperty(name = "notexistingproperty", stringValue = "true", lookupIfMissing = true)
    public static class WillBeEnabledLookupPropertyFilter {

        @ServerRequestFilter(priority = Priorities.USER + 20)
        public void voidRequestFilter(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("request-filters", "void-lookup-on");
        }

        @ServerResponseFilter(priority = Priorities.USER + 20)
        public void voidResponseFilter(ContainerResponseContext ctx) {
            ctx.getHeaders().add("response-filters", "void-lookup-on");
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
