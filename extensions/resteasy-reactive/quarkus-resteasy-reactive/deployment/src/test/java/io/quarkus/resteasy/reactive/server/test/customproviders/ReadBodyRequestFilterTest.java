package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ReadBodyRequestFilterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class);
                }
            });

    @Test
    public void testMethodWithBody() {
        RestAssured.with()
                .formParam("name", "Quarkus")
                .post("/hello")
                .then().body(Matchers.equalTo("hello Quarkus!!!!!!!"));
    }

    @Test
    public void testMethodWithoutBody() {
        RestAssured.with()
                .queryParam("name", "Quarkus")
                .get("/hello")
                .then().body(Matchers.equalTo("hello Quarkus!"));
    }

    @Path("hello")
    public static class HelloResource {

        @POST
        public String helloPost(@RestForm String name, HttpHeaders headers) {
            return "hello " + name + headers.getHeaderString("suffix");
        }

        @GET
        public String helloGet(@RestQuery String name, HttpHeaders headers) {
            return "hello " + name + headers.getHeaderString("suffix");
        }
    }

    public static class Filters {

        @ServerRequestFilter(readBody = true)
        public void addSuffix(ResteasyReactiveContainerRequestContext containerRequestContext) {
            ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) containerRequestContext
                    .getServerRequestContext();
            if (containerRequestContext.getMethod().equals("POST")) {
                String nameFormParam = (String) rrContext.getFormParameter("name", true, false);
                containerRequestContext.getHeaders().putSingle("suffix", "!".repeat(nameFormParam.length()));
            } else {
                containerRequestContext.getHeaders().putSingle("suffix", "!");
            }
        }
    }
}
