package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.function.Supplier;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class Issue22408TestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MyFilters.class, MyResource.class);
                }
            });

    @Test
    public void simpleTest() {
        RestAssured.get("/")
                .then().statusCode(204);
    }

    public static class MyFilters {

        @ServerExceptionMapper
        public Response mapException(RuntimeException x) {
            // this should get us a 204 which masks the original 405
            return null;
        }

        @ServerResponseFilter
        public void responseFilter(ContainerResponseContext ctx) {
            // NPE because the response was not set yet
            ctx.getEntity();
        }
    }

    @Path("/")
    public static class MyResource {
        // by calling a GET here we generate a 405
        @POST
        public String get() {
            return "Hello";
        }
    }
}
