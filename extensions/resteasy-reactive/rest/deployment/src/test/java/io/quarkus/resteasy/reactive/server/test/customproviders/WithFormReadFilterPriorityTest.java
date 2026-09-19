package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class WithFormReadFilterPriorityTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, Filters.class);
                }
            });

    @Test
    public void testFormReadFiltersRunInPriorityOrder() {
        RestAssured.with()
                .formParam("name", "Quarkus")
                .post("/hello")
                .then().body(Matchers.equalTo("hello Quarkus first/second/third"));
    }

    @Path("hello")
    public static class HelloResource {

        @POST
        public String helloPost(@RestForm String name, HttpHeaders headers) {
            return "hello " + name + " " + headers.getHeaderString("order");
        }
    }

    public static class Filters {

        @WithFormRead
        @ServerRequestFilter(readBody = true, priority = Priorities.USER + 100)
        public void first(ResteasyReactiveContainerRequestContext context) {
            append(context, "first");
        }

        @WithFormRead
        @ServerRequestFilter(readBody = true, priority = Priorities.USER + 200)
        public void second(ResteasyReactiveContainerRequestContext context) {
            append(context, "second");
        }

        @WithFormRead
        @ServerRequestFilter(readBody = true, priority = Priorities.USER + 300)
        public void third(ResteasyReactiveContainerRequestContext context) {
            append(context, "third");
        }

        private static void append(ResteasyReactiveContainerRequestContext context, String name) {
            String current = context.getHeaders().getFirst("order");
            context.getHeaders().putSingle("order", current == null ? name : current + "/" + name);
        }
    }
}
