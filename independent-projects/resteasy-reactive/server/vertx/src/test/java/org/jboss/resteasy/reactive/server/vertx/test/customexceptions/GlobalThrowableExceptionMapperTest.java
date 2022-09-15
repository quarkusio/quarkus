package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.*;
import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class GlobalThrowableExceptionMapperTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, ThrowableExceptionMapper.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/test/throwable")
                .then().statusCode(415);
    }

    @Path("test")
    public static class Resource {
        @GET
        @Path("throwable")
        @Produces("text/plain")
        public String throwsThrowable() throws Throwable {
            throw removeStackTrace(new Throwable());
        }
    }

    public static class ThrowableExceptionMapper {

        @ServerExceptionMapper
        public Response handleThrowable(Throwable t) {
            return Response.status(415).build();
        }
    }
}
