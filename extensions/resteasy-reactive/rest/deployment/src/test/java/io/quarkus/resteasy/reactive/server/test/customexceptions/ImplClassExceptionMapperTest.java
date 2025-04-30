package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ImplClassExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(CustomResourceImpl.class,
                                    CustomResource.class,
                                    GlobalCustomResourceImpl.class,
                                    GlobalCustomResource.class,
                                    DefaultCustomResource.class,
                                    ExceptionUtil.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/custom/error")
                .then().statusCode(416);
        RestAssured.get("/stock/error")
                .then().statusCode(500);
        RestAssured.get("/default/error")
                .then().statusCode(417);
    }

    @Path("default")
    public static class DefaultCustomResource {

        @ServerExceptionMapper
        public Response handleThrowable(RuntimeException t) {
            return Response.status(417).build();
        }

        @GET
        @Path("error")
        @Produces("text/plain")
        public String throwsException() {
            throw removeStackTrace(new RuntimeException());
        }
    }

    public static class CustomResourceImpl implements CustomResource {

        @ServerExceptionMapper
        public Response handleThrowable(RuntimeException t) {
            return Response.status(416).build();
        }

        public String throwsException() {
            throw removeStackTrace(new RuntimeException());
        }
    }

    @Path("custom")
    public interface CustomResource {
        @GET
        @Path("error")
        @Produces("text/plain")
        String throwsException();
    }

    public static class GlobalCustomResourceImpl implements GlobalCustomResource {

        public String throwsException() {
            throw removeStackTrace(new RuntimeException());
        }
    }

    @Path("stock")
    public interface GlobalCustomResource {

        @GET
        @Path("error")
        @Produces("text/plain")
        String throwsException();
    }
}
