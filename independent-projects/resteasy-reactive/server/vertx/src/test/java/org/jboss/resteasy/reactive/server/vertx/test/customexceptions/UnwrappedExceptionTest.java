package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.scanning.FeatureScanner;
import org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class UnwrappedExceptionTest {
    public static final int NO_MAPPER_FOUND_STATUS = 500;
    public static final int WRAPPER_MAPPER_USED_STATUS = 977;
    public static final int INTERMEDIATE_MAPPER_USED_STATUS = 988;
    public static final int WRAPPED_MAPPER_USED_STATUS = 999;

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.addFeatureScanner(new FeatureScanner() {
                        @Override
                        public FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication) {
                            scannedApplication.getExceptionMappers().addUnwrappedException(
                                    UnwrapAlwaysFalseException.class.getName(),
                                    false);
                            scannedApplication.getExceptionMappers().addUnwrappedException(
                                    UnwrapAlwaysFalseWithIntermediateException.class.getName(),
                                    false);
                            scannedApplication.getExceptionMappers()
                                    .addUnwrappedException(UnwrapAlwaysTrueException.class.getName(), true);
                            scannedApplication.getExceptionMappers()
                                    .addUnwrappedException(UnwrapAlwaysTrueDirectlyMappedException.class.getName(), true);
                            return new FeatureScanResult(List.of());
                        }
                    });
                }
            })
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ExceptionResource.class, ExceptionMappers.class, ExceptionUtil.class,
                                    UnwrapAlwaysFalseException.class);
                }
            });

    @Test
    public void testWrapperOfUnmapped() {
        // Exception is wrapping an unmapped exception
        // => No mapper should be used, error 500
        RestAssured.get("/hello/wrapperOfUnmapped")
                .then().statusCode(NO_MAPPER_FOUND_STATUS);
    }

    @Test
    public void testWrapperAlwaysFalseOfMapped() {
        // Exception is wrapping a mapped exception with always=false
        // => Wrapped exception mapper should be used since UnwrapAlwaysFalseException is not mapped directly
        RestAssured.get("/hello/wrapperAlwaysFalseOfMapped")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testWrapperAlwaysFalseWithIntermediateOfMapped() {
        // Exception is wrapping a mapped exception with always=false
        // => Intermediate exception mapper should be used since UnwrapAlwaysFalseWithIntermediateException is not mapped directly but its child is
        RestAssured.get("/hello/wrapperAlwaysFalseWithIntermediateOfMapped")
                .then().statusCode(INTERMEDIATE_MAPPER_USED_STATUS);
    }

    @Test
    public void testUnmapped() {
        // Exception is unmapped
        // => No mapper should be used, error 500
        RestAssured.get("/hello/unmapped")
                .then().statusCode(NO_MAPPER_FOUND_STATUS);
    }

    @Test
    public void testWrappedDirectlyThrown() {
        // Exception is thrown directly
        // => Its specific exception mapper should be used
        RestAssured.get("/hello/wrappedDirectlyThrown")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testIntermediateThrownWithoutWrappedChild() {
        // Exception is thrown without a wrapped child
        // => Its specific exception mapper should be used
        RestAssured.get("/hello/intermediateThrownWithoutWrappedChild")
                .then().statusCode(INTERMEDIATE_MAPPER_USED_STATUS);
    }

    @Test
    public void testWrapperAlwaysTrueOfMapped() {
        // Exception is wrapping a mapped exception with always=true
        // => Wrapped exception mapper should be used
        RestAssured.get("/hello/wrapperAlwaysTrueOfMapped")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testWrapperAlwaysTrueDirectlyMapped() {
        // Exception is wrapping a mapped exception with always=true, but is also directly mapped
        // => Direct mapper should be used
        RestAssured.get("/hello/wrapperAlwaysTrueDirectlyMapped")
                .then().statusCode(WRAPPER_MAPPER_USED_STATUS);
    }

    @Path("hello")
    public static class ExceptionResource {

        @Path("wrapperOfUnmapped")
        public String wrapperOfUnmapped() {
            throw removeStackTrace(new UnwrapAlwaysFalseException(removeStackTrace(new IllegalArgumentException())));
        }

        @Path("wrapperAlwaysFalseOfMapped")
        public String wrapperAlwaysFalseOfMapped() {
            throw removeStackTrace(new UnwrapAlwaysFalseException(removeStackTrace(new WrappedException())));
        }

        @Path("wrapperAlwaysFalseWithIntermediateOfMapped")
        public String wrapperAlwaysFalseWithIntermediateOfMapped() {
            throw removeStackTrace(new UnwrapAlwaysFalseWithIntermediateException(removeStackTrace(new WrappedException())));
        }

        @Path("unmapped")
        public String unmapped() {
            throw removeStackTrace(new IllegalArgumentException());
        }

        @Path("wrappedDirectlyThrown")
        public String wrappedDirectlyThrown() {
            throw removeStackTrace(new WrappedException());
        }

        @Path("intermediateThrownWithoutWrappedChild")
        public String intermediateThrownWithoutWrappedChild() {
            throw removeStackTrace(new UnwrapAlwaysTrueException());
        }

        @Path("wrapperAlwaysTrueOfMapped")
        public String wrapperAlwaysTrueOfMapped() {
            throw removeStackTrace(new UnwrapAlwaysTrueException(removeStackTrace(new WrappedException())));
        }

        @Path("wrapperAlwaysTrueDirectlyMapped")
        public String wrapperAlwaysTrueDirectlyMapped() {
            throw removeStackTrace(new UnwrapAlwaysTrueDirectlyMappedException(removeStackTrace(new WrappedException())));
        }
    }

    public static class ExceptionMappers {
        @ServerExceptionMapper
        Response map(WrappedException e) {
            return Response.status(WRAPPED_MAPPER_USED_STATUS).build();
        }

        @ServerExceptionMapper
        Response map(IntermediateMappedException e) {
            return Response.status(INTERMEDIATE_MAPPER_USED_STATUS).build();
        }

        @ServerExceptionMapper
        Response map(UnwrapAlwaysTrueDirectlyMappedException e) {
            return Response.status(WRAPPER_MAPPER_USED_STATUS).build();
        }
    }

    public static class WrappedException extends RuntimeException {
    }

    public static class UnwrapAlwaysFalseException extends RuntimeException {
        public UnwrapAlwaysFalseException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapAlwaysFalseWithIntermediateException extends IntermediateMappedException {
        public UnwrapAlwaysFalseWithIntermediateException(Throwable cause) {
            super(cause);
        }
    }

    public static class IntermediateMappedException extends RuntimeException {
        public IntermediateMappedException() {
        }

        public IntermediateMappedException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapAlwaysTrueException extends IntermediateMappedException {
        public UnwrapAlwaysTrueException() {
        }

        public UnwrapAlwaysTrueException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapAlwaysTrueDirectlyMappedException extends IntermediateMappedException {
        public UnwrapAlwaysTrueDirectlyMappedException(Throwable cause) {
            super(cause);
        }
    }
}
