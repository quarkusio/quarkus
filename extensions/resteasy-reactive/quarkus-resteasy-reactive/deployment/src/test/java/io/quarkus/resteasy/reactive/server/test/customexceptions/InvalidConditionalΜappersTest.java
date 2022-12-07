package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class InvalidConditionalΜappersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class, Mappers.class);
                }
            }).assertException(t -> {
                String message = t.getMessage();
                assertTrue(message.contains("@ServerExceptionMapper"));
                assertTrue(message.contains("request"));
                assertTrue(message.contains(Mappers.class.getName()));
            });

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String hello() {
            return "hello";
        }

    }

    public static class Mappers {

        @IfBuildProfile("test")
        @ServerExceptionMapper
        public Response request(IllegalArgumentException ignored) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }
}
