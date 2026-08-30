package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ReadBodyRequestFilterOrderTest {

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
    public void readBodyRequestFiltersKeepPriorityOrder() {
        RestAssured.with()
                .formParam("name", "Quarkus")
                .post("/hello")
                .then().body(Matchers.equalTo("first,second:Quarkus"));
    }

    @Path("hello")
    public static class HelloResource {

        @POST
        public String hello(@RestForm String name, HttpHeaders headers) {
            List<String> order = headers.getRequestHeader("filter-order");
            return String.join(",", order) + ":" + name;
        }
    }

    public static class Filters {

        @WithFormRead
        @ServerRequestFilter(priority = Priorities.USER + 1)
        public void first(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("filter-order", "first");
        }

        @WithFormRead
        @ServerRequestFilter(priority = Priorities.USER + 2)
        public void second(ContainerRequestContext requestContext) {
            requestContext.getHeaders().add("filter-order", "second");
        }
    }
}
