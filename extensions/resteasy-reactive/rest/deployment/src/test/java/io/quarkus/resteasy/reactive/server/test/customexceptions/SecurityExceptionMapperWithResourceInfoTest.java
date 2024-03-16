package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import jakarta.annotation.security.DenyAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SecurityExceptionMapperWithResourceInfoTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    void test() {
        RestAssured.get("/test/denied")
                .then().statusCode(403).body(is(Resource.class.getName()));
    }

    @Path("test")
    public static class Resource {
        @GET
        @Path("denied")
        @Produces("text/plain")
        @DenyAll
        public String denied() {
            return "denied";
        }

        @ServerExceptionMapper(SecurityException.class)
        Response handle(SecurityException t, ResourceInfo resourceInfo) {
            return Response.status(403).entity(resourceInfo.getResourceClass().getName()).build();
        }
    }

}
