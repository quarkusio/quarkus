package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WithFormBodyTest {

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
                .then().body(Matchers.equalTo("hello Quarkus"));
    }

    @Test
    public void testMethodWithUndeclaredBody() {
        RestAssured.with()
                .formParam("name", "Quarkus")
                .post("/hello/empty")
                .then().body(Matchers.equalTo("hello Quarkus"));
    }

    @Path("hello")
    public static class HelloResource {

        @POST
        public String helloPost(@RestForm String name) {
            return "hello " + name;
        }

        @WithFormRead
        @Path("empty")
        @POST
        public String helloEmptyPost(ServerRequestContext requestContext) {
            return "hello " + ((ResteasyReactiveRequestContext) requestContext).getFormParameter("name", true, false);
        }
    }
}
