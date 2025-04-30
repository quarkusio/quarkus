package io.quarkus.resteasy.reactive.server.test.customexceptions;

import static io.restassured.RestAssured.*;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ConditionalExceptionMappersTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(AbstractException.class, FirstException.class, SecondException.class,
                                    WontBeEnabledMappers.class, WillBeEnabledMappers.class, AlwaysEnabledMappers.class,
                                    TestResource.class);
                }
            });

    @Test
    public void test() {
        get("/first").then().statusCode(903);
        get("/second").then().statusCode(801);
        get("/third").then().statusCode(555);
    }

    @Path("")
    public static class TestResource {

        @Path("first")
        @GET
        public String first() {
            throw new FirstException();
        }

        @Path("second")
        @GET
        public String second() {
            throw new SecondException();
        }

        @Path("third")
        @GET
        public String third() {
            throw new ThirdException();
        }
    }

    public static abstract class AbstractException extends RuntimeException {

        public AbstractException() {
            setStackTrace(new StackTraceElement[0]);
        }
    }

    public static class FirstException extends AbstractException {

    }

    public static class SecondException extends AbstractException {

    }

    public static class ThirdException extends AbstractException {

    }

    @IfBuildProfile("dummy")
    public static class WontBeEnabledMappers {

        @ServerExceptionMapper(FirstException.class)
        public Response first() {
            return Response.status(900).build();
        }

        @ServerExceptionMapper(value = FirstException.class, priority = Priorities.USER - 100)
        public Response firstWithLowerPriority() {
            return Response.status(901).build();
        }

        @ServerExceptionMapper(priority = Priorities.USER - 100)
        public Response second(SecondException ignored) {
            return Response.status(800).build();
        }
    }

    @LookupUnlessProperty(name = "notexistingproperty", stringValue = "true", lookupIfMissing = true)
    public static class WillBeEnabledMappers {

        @ServerExceptionMapper(value = FirstException.class, priority = Priorities.USER + 10)
        public Response first() {
            return Response.status(902).build();
        }

        @ServerExceptionMapper(value = FirstException.class, priority = Priorities.USER - 10)
        public Response firstWithLowerPriority() {
            return Response.status(903).build();
        }

        @ServerExceptionMapper(priority = Priorities.USER - 10)
        public RestResponse<Void> second(SecondException ignored) {
            return RestResponse.status(801);
        }
    }

    public static class AlwaysEnabledMappers {

        @ServerExceptionMapper(value = FirstException.class, priority = Priorities.USER + 1000)
        public Response first() {
            return Response.status(555).build();
        }

        @ServerExceptionMapper(value = SecondException.class, priority = Priorities.USER + 1000)
        public Response second() {
            return Response.status(555).build();
        }

        @ServerExceptionMapper(value = ThirdException.class, priority = Priorities.USER + 1000)
        public Uni<Response> third() {
            return Uni.createFrom().item(Response.status(555).build());
        }
    }
}
