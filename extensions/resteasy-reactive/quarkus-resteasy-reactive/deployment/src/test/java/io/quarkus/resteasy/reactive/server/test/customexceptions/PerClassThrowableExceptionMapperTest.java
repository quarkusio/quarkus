package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PerClassThrowableExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HasCustomThrowableHandlerResource.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/custom/throwable")
                .then().statusCode(416);

        RestAssured.get("/stock/throwable")
                .then().statusCode(500);
    }

    @Path("custom")
    public static class HasCustomThrowableHandlerResource {

        @ServerExceptionMapper
        public Response handleThrowable(Throwable t) {
            return Response.status(416).build();
        }

        @GET
        @Path("throwable")
        @Produces("text/plain")
        public String throwsThrowable() throws Throwable {
            throw removeStackTrace(new Throwable());
        }
    }

    @Path("stock")
    public static class DoesNotHaveCustomThrowableHandlerResource {

        @GET
        @Path("throwable")
        @Produces("text/plain")
        public String throwsThrowable() throws Throwable {
            throw removeStackTrace(new Throwable());
        }
    }
}
