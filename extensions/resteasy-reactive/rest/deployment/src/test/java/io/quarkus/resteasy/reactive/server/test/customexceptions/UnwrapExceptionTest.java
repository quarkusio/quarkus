package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.quarkus.resteasy.reactive.server.test.ExceptionUtil.removeStackTrace;
import static io.restassured.RestAssured.when;

import java.util.function.Supplier;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.UnwrapException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class UnwrapExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FirstException.class, SecondException.class, ThirdException.class,
                                    FourthException.class, FifthException.class, SixthException.class,
                                    Mappers.class, Resource.class, ExceptionUtil.class);
                }
            });

    @Test
    public void testWrapperWithUnmappedException() {
        when().get("/hello/iaeInSecond")
                .then().statusCode(500);
    }

    @Test
    public void testMappedExceptionWithoutUnwrappedWrapper() {
        when().get("/hello/iseInFirst")
                .then().statusCode(500);

        when().get("/hello/iseInThird")
                .then().statusCode(500);

        when().get("/hello/iseInSixth")
                .then().statusCode(500);
    }

    @Test
    public void testWrapperWithMappedException() {
        when().get("/hello/iseInSecond")
                .then().statusCode(900);

        when().get("/hello/iseInFourth")
                .then().statusCode(900);

        when().get("/hello/iseInFifth")
                .then().statusCode(900);
    }

    @Path("hello")
    public static class Resource {

        @Path("iseInFirst")
        public String throwsISEAsCauseOfFirstException() {
            throw removeStackTrace(new FirstException(removeStackTrace(new IllegalStateException("dummy"))));
        }

        @Path("iseInSecond")
        public String throwsISEAsCauseOfSecondException() {
            throw removeStackTrace(new SecondException(removeStackTrace(new IllegalStateException("dummy"))));
        }

        @Path("iaeInSecond")
        public String throwsIAEAsCauseOfSecondException() {
            throw removeStackTrace(new SecondException(removeStackTrace(new IllegalArgumentException("dummy"))));
        }

        @Path("iseInThird")
        public String throwsISEAsCauseOfThirdException() throws ThirdException {
            throw removeStackTrace(new ThirdException(removeStackTrace(new IllegalStateException("dummy"))));
        }

        @Path("iseInFourth")
        public String throwsISEAsCauseOfFourthException() throws FourthException {
            throw removeStackTrace(new FourthException(removeStackTrace(new IllegalStateException("dummy"))));
        }

        @Path("iseInFifth")
        public String throwsISEAsCauseOfFifthException() {
            throw removeStackTrace(new FifthException(removeStackTrace(new IllegalStateException("dummy"))));
        }

        @Path("iseInSixth")
        public String throwsISEAsCauseOfSixthException() {
            throw removeStackTrace(new SixthException(removeStackTrace(new IllegalStateException("dummy"))));
        }
    }

    @UnwrapException({ FourthException.class, FifthException.class })
    public static class Mappers {

        @ServerExceptionMapper
        public Response handleIllegalStateException(IllegalStateException e) {
            return Response.status(900).build();
        }
    }

    public static class FirstException extends RuntimeException {

        public FirstException(Throwable cause) {
            super(cause);
        }
    }

    @UnwrapException
    public static class SecondException extends FirstException {

        public SecondException(Throwable cause) {
            super(cause);
        }
    }

    public static class ThirdException extends Exception {

        public ThirdException(Throwable cause) {
            super(cause);
        }
    }

    public static class FourthException extends SecondException {

        public FourthException(Throwable cause) {
            super(cause);
        }
    }

    public static class FifthException extends RuntimeException {

        public FifthException(Throwable cause) {
            super(cause);
        }
    }

    public static class SixthException extends RuntimeException {

        public SixthException(Throwable cause) {
            super(cause);
        }
    }
}
