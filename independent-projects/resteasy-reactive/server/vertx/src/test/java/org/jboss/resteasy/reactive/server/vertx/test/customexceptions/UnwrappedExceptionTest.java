package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.ExceptionUnwrapStrategy;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
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
                            ExceptionMapping exceptionMapping = scannedApplication.getExceptionMappers();
                            exceptionMapping.addUnwrappedException(
                                    UnwrapIfNoMatchException.class.getName(),
                                    ExceptionUnwrapStrategy.UNWRAP_IF_NO_MATCH);
                            exceptionMapping.addUnwrappedException(
                                    UnwrapIfNoMatchWithIntermediateException.class.getName(),
                                    ExceptionUnwrapStrategy.UNWRAP_IF_NO_MATCH);
                            exceptionMapping.addUnwrappedException(
                                    UnwrapIfNoExactMatchException.class.getName(),
                                    ExceptionUnwrapStrategy.UNWRAP_IF_NO_EXACT_MATCH);
                            exceptionMapping.addUnwrappedException(
                                    UnwrapIfNoExactMatchDirectlyMappedException.class.getName(),
                                    ExceptionUnwrapStrategy.UNWRAP_IF_NO_EXACT_MATCH);
                            exceptionMapping.addUnwrappedException(
                                    UnwrapAlwaysException.class.getName(),
                                    ExceptionUnwrapStrategy.ALWAYS);
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
                                    UnwrapIfNoMatchException.class);
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
    public void testWrapperIfNoMatchOfMapped() {
        // Exception is wrapping a mapped exception with UNWRAP_IF_NO_MATCH strategy
        // => Wrapped exception mapper should be used since UnwrapIfNoMatchException is not mapped directly
        RestAssured.get("/hello/wrapperIfNoMatchOfMapped")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testWrapperIfNoMatchWithIntermediateOfMapped() {
        // Exception is wrapping a mapped exception with UNWRAP_IF_NO_MATCH strategy
        // => Intermediate exception mapper should be used since UnwrapIfNoMatchWithIntermediateException is not mapped directly but its child is
        RestAssured.get("/hello/wrapperIfNoMatchWithIntermediateOfMapped")
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
    public void testWrapperIfNoExactMatchOfMapped() {
        // Exception is wrapping a mapped exception with UNWRAP_IF_NO_EXACT_MATCH strategy
        // => Wrapped exception mapper should be used
        RestAssured.get("/hello/wrapperIfNoExactMatchOfMapped")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testWrapperIfNoExactMatchDirectlyMapped() {
        // Exception is wrapping a mapped exception with UNWRAP_IF_NO_EXACT_MATCH strategy, but is also directly mapped
        // => Direct mapper should be used
        RestAssured.get("/hello/wrapperIfNoExactMatchDirectlyMapped")
                .then().statusCode(WRAPPER_MAPPER_USED_STATUS);
    }

    @Test
    public void testAlwaysUnwrapsEvenWithParentMapper() {
        // Exception with ALWAYS strategy wrapping a specific mapped exception
        // Even though UnwrapAlwaysException extends IntermediateMappedException (which has a mapper),
        // it should unwrap and use the SpecificException mapper
        RestAssured.get("/hello/alwaysUnwrapsEvenWithParentMapper")
                .then().statusCode(WRAPPED_MAPPER_USED_STATUS);
    }

    @Test
    public void testAlwaysFallsBackToWrapperWhenNoCauseMapper() {
        // Exception with ALWAYS strategy wrapping an unmapped exception
        // Should fall back to the wrapper's parent mapper (IntermediateMappedException)
        RestAssured.get("/hello/alwaysFallsBackToWrapperWhenNoCauseMapper")
                .then().statusCode(WRAPPER_MAPPER_USED_STATUS);
    }

    @Path("hello")
    public static class ExceptionResource {

        @Path("wrapperOfUnmapped")
        public String wrapperOfUnmapped() {
            throw removeStackTrace(new UnwrapIfNoMatchException(removeStackTrace(new IllegalArgumentException())));
        }

        @Path("wrapperIfNoMatchOfMapped")
        public String wrapperIfNoMatchOfMapped() {
            throw removeStackTrace(new UnwrapIfNoMatchException(removeStackTrace(new WrappedException())));
        }

        @Path("wrapperIfNoMatchWithIntermediateOfMapped")
        public String wrapperIfNoMatchWithIntermediateOfMapped() {
            throw removeStackTrace(new UnwrapIfNoMatchWithIntermediateException(removeStackTrace(new WrappedException())));
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
            throw removeStackTrace(new UnwrapIfNoExactMatchException());
        }

        @Path("wrapperIfNoExactMatchOfMapped")
        public String wrapperIfNoExactMatchOfMapped() {
            throw removeStackTrace(new UnwrapIfNoExactMatchException(removeStackTrace(new WrappedException())));
        }

        @Path("wrapperIfNoExactMatchDirectlyMapped")
        public String wrapperIfNoExactMatchDirectlyMapped() {
            throw removeStackTrace(new UnwrapIfNoExactMatchDirectlyMappedException(removeStackTrace(new WrappedException())));
        }

        @Path("alwaysUnwrapsEvenWithParentMapper")
        public String alwaysUnwrapsEvenWithParentMapper() {
            throw removeStackTrace(new UnwrapAlwaysException(removeStackTrace(new WrappedException())));
        }

        @Path("alwaysFallsBackToWrapperWhenNoCauseMapper")
        public String alwaysFallsBackToWrapperWhenNoCauseMapper() {
            throw removeStackTrace(new UnwrapAlwaysException(removeStackTrace(new IllegalArgumentException())));
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
        Response map(UnwrapIfNoExactMatchDirectlyMappedException e) {
            return Response.status(WRAPPER_MAPPER_USED_STATUS).build();
        }

        @ServerExceptionMapper
        Response map(UnwrapAlwaysException e) {
            return Response.status(WRAPPER_MAPPER_USED_STATUS).build();
        }
    }

    public static class WrappedException extends RuntimeException {
    }

    public static class UnwrapIfNoMatchException extends RuntimeException {
        public UnwrapIfNoMatchException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapIfNoMatchWithIntermediateException extends IntermediateMappedException {
        public UnwrapIfNoMatchWithIntermediateException(Throwable cause) {
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

    public static class UnwrapIfNoExactMatchException extends IntermediateMappedException {
        public UnwrapIfNoExactMatchException() {
        }

        public UnwrapIfNoExactMatchException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapIfNoExactMatchDirectlyMappedException extends IntermediateMappedException {
        public UnwrapIfNoExactMatchDirectlyMappedException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnwrapAlwaysException extends IntermediateMappedException {
        public UnwrapAlwaysException(Throwable cause) {
            super(cause);
        }
    }
}
