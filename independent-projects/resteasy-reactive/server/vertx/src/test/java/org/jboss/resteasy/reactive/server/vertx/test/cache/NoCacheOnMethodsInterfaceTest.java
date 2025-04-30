package org.jboss.resteasy.reactive.server.vertx.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.scanning.CacheControlScanner;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class NoCacheOnMethodsInterfaceTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.addMethodScanner(new CacheControlScanner());
                }
            })
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(IResourceWithNoCache.class, ResourceWithNoCache.class);
                }
            });

    @Test
    public void testWithFields() {
        RestAssured.get("/test/withFields")
                .then()
                .statusCode(200)
                .body(equalTo("withFields"))
                .header("Cache-Control", "no-cache=\"f1\", no-cache=\"f2\"");
    }

    @Test
    public void testWithoutFields() {
        RestAssured.get("/test/withoutFields")
                .then()
                .statusCode(200)
                .body(equalTo("withoutFields"))
                .header("Cache-Control", "no-cache");
    }

    @Test
    public void testWithoutAnnotation() {
        RestAssured.get("/test/withoutAnnotation")
                .then()
                .statusCode(200)
                .body(equalTo("withoutAnnotation"))
                .header("Cache-Control", nullValue());
    }

    @Path("test")
    public interface IResourceWithNoCache {

        @Path("withFields")
        @GET
        @NoCache(fields = { "f1", "f2" })
        String withFields();

        @Path("withoutFields")
        @GET
        @NoCache
        String withoutFields();

        @Path("withoutAnnotation")
        @GET
        String withoutAnnotation();
    }

    public static class ResourceWithNoCache implements IResourceWithNoCache {

        @Override
        public String withFields() {
            return "withFields";
        }

        @Override
        public String withoutFields() {
            return "withoutFields";
        }

        @Override
        public String withoutAnnotation() {
            return "withoutAnnotation";
        }
    }
}
