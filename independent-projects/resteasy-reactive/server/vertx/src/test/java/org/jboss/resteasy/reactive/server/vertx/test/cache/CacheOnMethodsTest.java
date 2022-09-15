package org.jboss.resteasy.reactive.server.vertx.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.scanning.CacheControlScanner;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CacheOnMethodsTest {

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
                    return ShrinkWrap.create(JavaArchive.class).addClasses(ResourceWithCache.class);
                }
            });

    @Test
    public void testWith() {
        RestAssured.get("/test/with")
                .then()
                .statusCode(200)
                .body(equalTo("with"))
                .header("Cache-Control", "must-revalidate, no-store, max-age=100, private");
    }

    @Test
    public void testWithout() {
        RestAssured.get("/test/without")
                .then()
                .statusCode(200)
                .body(equalTo("without"))
                .header("Cache-Control", nullValue());
    }

    @Path("test")
    public static class ResourceWithCache {

        @Path("with")
        @GET
        @Cache(maxAge = 100, noStore = true, mustRevalidate = true, isPrivate = true)
        public String with() {
            return "with";
        }

        @Path("without")
        @GET
        public String without() {
            return "without";
        }
    }
}
