package io.quarkus.resteasy.reactive.server.test.customproviders;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AsyncResponseWithExceptionAndFiltersTargetTest {

    private static final String RESOURCE_INFO_CLASS_HEADER = "resourceInfoClass";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(CsResource.class, UniResource.class,
                                    CustomResponseFilter.class,
                                    DummyException.class, DummyExceptionMapper.class,
                                    DummyException2.class, DummyException2.class);
                }
            });

    @Path("cs")
    public static class CsResource {

        @GET
        @Path("handled")
        public CompletionStage<String> handled() {
            throw new DummyException(true);
        }

        @GET
        @Path("handled2")
        public CompletionStage<String> handled2() {
            throw new DummyException2(true);
        }

        @GET
        @Path("unhandled")
        public CompletionStage<String> unhandled() {
            throw new DummyException(false);
        }

        @GET
        @Path("unhandled2")
        public CompletionStage<String> unhandled2() {
            throw new DummyException2(false);
        }
    }

    @Path("uni")
    public static class UniResource {

        @GET
        @Path("handled")
        public CompletionStage<String> handled() {
            throw new DummyException(true);
        }

        @GET
        @Path("handled2")
        public CompletionStage<String> handled2() {
            throw new DummyException2(true);
        }

        @GET
        @Path("unhandled")
        public CompletionStage<String> unhandled() {
            throw new DummyException(false);
        }

        @GET
        @Path("unhandled2")
        public CompletionStage<String> unhandled2() {
            throw new DummyException2(false);
        }
    }

    @Test
    public void csHandled() {
        when().get("/cs/handled")
                .then()
                .statusCode(999)
                .header(RESOURCE_INFO_CLASS_HEADER, CsResource.class.getSimpleName());
    }

    @Test
    public void csHandled2() {
        when().get("/cs/handled2")
                .then()
                .statusCode(999)
                .header(RESOURCE_INFO_CLASS_HEADER, CsResource.class.getSimpleName());
    }

    @Test
    public void csUnhandled() {
        when().get("/cs/unhandled")
                .then()
                .statusCode(500)
                .header(RESOURCE_INFO_CLASS_HEADER, nullValue());
    }

    @Test
    public void csUnhandled2() {
        when().get("/cs/unhandled2")
                .then()
                .statusCode(204)
                .header(RESOURCE_INFO_CLASS_HEADER, CsResource.class.getSimpleName());
    }

    @Test
    public void uniHandled() {
        when().get("/uni/handled")
                .then()
                .statusCode(999)
                .header(RESOURCE_INFO_CLASS_HEADER, UniResource.class.getSimpleName());
    }

    @Test
    public void uniHandled2() {
        when().get("/uni/handled2")
                .then()
                .statusCode(999)
                .header(RESOURCE_INFO_CLASS_HEADER, UniResource.class.getSimpleName());
    }

    @Test
    public void uniUnhandled() {
        when().get("/uni/unhandled")
                .then()
                .statusCode(500)
                .header(RESOURCE_INFO_CLASS_HEADER, nullValue());
    }

    @Test
    public void uniUnhandled2() {
        when().get("/uni/unhandled2")
                .then()
                .statusCode(204)
                .header(RESOURCE_INFO_CLASS_HEADER, UniResource.class.getSimpleName());
    }

    public static class CustomResponseFilter {

        @ServerResponseFilter
        public void filter(ContainerResponseContext responseContext, ResourceInfo resourceInfo) {
            responseContext.getHeaders().add(RESOURCE_INFO_CLASS_HEADER, resourceInfo.getResourceClass().getSimpleName());
        }
    }

    @Provider
    public static class DummyExceptionMapper implements ExceptionMapper<DummyException> {

        @Override
        public Response toResponse(DummyException exception) {
            if (exception.isHandle()) {
                return Response.status(999).build();
            }
            throw exception;
        }
    }

    public static class DummyExceptionMapper2 {

        @ServerExceptionMapper
        public Response handle(DummyException2 ex) {
            if (ex.isHandle()) {
                return Response.status(999).build();
            }
            return null;
        }
    }

    public static class DummyException extends RuntimeException {

        private final boolean handle;

        public DummyException(boolean handle) {
            super("dummy");
            this.handle = handle;
            setStackTrace(new StackTraceElement[0]);
        }

        public boolean isHandle() {
            return handle;
        }
    }

    public static class DummyException2 extends RuntimeException {

        private final boolean handle;

        public DummyException2(boolean handle) {
            super("dummy2");
            this.handle = handle;
            setStackTrace(new StackTraceElement[0]);
        }

        public boolean isHandle() {
            return handle;
        }
    }
}
