package org.jboss.resteasy.reactive.server.vertx.test.customexceptions;

import static org.jboss.resteasy.reactive.server.vertx.test.ExceptionUtil.removeStackTrace;

import io.restassured.RestAssured;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

public class UnwrappedExceptionTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.addFeatureScanner(new FeatureScanner() {
                        @Override
                        public FeatureScanResult integrate(IndexView application, ScannedApplication scannedApplication) {
                            scannedApplication.getExceptionMappers().addUnwrappedException(TestUnwrapException.class.getName());
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
                                    TestUnwrapException.class);
                }
            });

    @Test
    public void testWrapperWithUnmappedException() {
        RestAssured.get("/hello/wrapperOfIAE")
                .then().statusCode(500);
    }

    @Test
    public void testWrapperWithMappedException() {
        RestAssured.get("/hello/wrapperOfISE")
                .then().statusCode(999);
    }

    @Test
    public void testUnmappedException() {
        RestAssured.get("/hello/iae")
                .then().statusCode(500);
    }

    @Test
    public void testMappedException() {
        RestAssured.get("/hello/ise")
                .then().statusCode(999);
    }

    @Path("hello")
    public static class ExceptionResource {

        @Path("wrapperOfIAE")
        public String wrapperOfIAE() {
            throw removeStackTrace(new TestUnwrapException(removeStackTrace(new IllegalArgumentException())));
        }

        @Path("wrapperOfISE")
        public String wrapperOfISE() {
            throw removeStackTrace(new TestUnwrapException(removeStackTrace(new IllegalStateException())));
        }

        @Path("iae")
        public String iae() {
            throw removeStackTrace(new IllegalArgumentException());
        }

        @Path("ise")
        public String ise() {
            throw removeStackTrace(new IllegalStateException());
        }
    }

    public static class ExceptionMappers {

        @ServerExceptionMapper
        Response mapISE(IllegalStateException e) {
            return Response.status(999).build();
        }
    }

    public static class TestUnwrapException extends RuntimeException {
        public TestUnwrapException() {
        }

        public TestUnwrapException(String message) {
            super(message);
        }

        public TestUnwrapException(String message, Throwable cause) {
            super(message, cause);
        }

        public TestUnwrapException(Throwable cause) {
            super(cause);
        }

        public TestUnwrapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
