package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Filters annotated with {@link WithFormRead} are moved after the form-reading handler.
 * That relocation must preserve their relative priority order.
 */
public class WithFormReadFilterOrderTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class);
                }
            });

    @Test
    public void filtersRunInPriorityOrderAfterFormRead() {
        RestAssured.with()
                .formParam("name", "Quarkus")
                .post("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("first(Quarkus)/second(Quarkus)"));
    }

    @Path("hello")
    public static class HelloResource {

        @POST
        public String hello(@RestForm String name, HttpHeaders headers) {
            return headers.getHeaderString("filter-request");
        }
    }

    public static class Filters {

        @WithFormRead
        @ServerRequestFilter(priority = Priorities.USER + 100)
        public void first(ResteasyReactiveContainerRequestContext requestContext) {
            appendMarker(requestContext, "first");
        }

        @WithFormRead
        @ServerRequestFilter(priority = Priorities.USER + 200)
        public void second(ResteasyReactiveContainerRequestContext requestContext) {
            appendMarker(requestContext, "second");
        }

        // records the marker together with the form value, which proves the filter ran after the form was read
        private static void appendMarker(ResteasyReactiveContainerRequestContext requestContext, String marker) {
            ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) requestContext
                    .getServerRequestContext();
            Object name = rrContext.getFormParameter("name", true, false);
            String entry = marker + "(" + name + ")";
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String previous = headers.getFirst("filter-request");
            headers.putSingle("filter-request", previous == null ? entry : previous + "/" + entry);
        }
    }
}
