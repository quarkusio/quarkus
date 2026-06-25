package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class GenericExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestException.class,
                                    AbstractGenericMapper.class,
                                    GenericMapperBean.class,
                                    SimpleMapperBean.class,
                                    TestResource.class,
                                    ExceptionUtil.class);
                }
            });

    @Test
    public void genericMapperShouldWork() {
        RestAssured.get("/test/generic")
                .then().statusCode(499);
    }

    @Test
    public void simpleMapperShouldNotBeDropped() {
        RestAssured.get("/test/non-generic")
                .then().statusCode(497);
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }
    }

    public abstract static class AbstractGenericMapper<E extends Exception> {
        public abstract RestResponse<?> toResponse(E e);
    }

    @ApplicationScoped
    public static class GenericMapperBean extends AbstractGenericMapper<TestException> {

        @ServerExceptionMapper(TestException.class)
        public RestResponse<?> toResponse(TestException e) {
            return RestResponse.status(499);
        }
    }

    @ApplicationScoped
    public static class SimpleMapperBean {

        @ServerExceptionMapper(IllegalArgumentException.class)
        public RestResponse<?> toResponse(IllegalArgumentException e) {
            return RestResponse.status(497);
        }
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/generic")
        @Produces("text/plain")
        public String throwGeneric() {
            throw removeStackTrace(new TestException("generic mapper test"));
        }

        @GET
        @Path("/non-generic")
        @Produces("text/plain")
        public String throwNonGeneric() {
            throw removeStackTrace(new IllegalArgumentException("non-generic mapper test"));
        }
    }
}
