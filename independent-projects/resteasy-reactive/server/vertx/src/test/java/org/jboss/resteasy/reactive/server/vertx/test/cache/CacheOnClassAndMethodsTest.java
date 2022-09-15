package org.jboss.resteasy.reactive.server.vertx.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;

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

public class CacheOnClassAndMethodsTest {

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
                            .addClasses(ResourceWithCache.class);
                }
            });

    @Test
    public void testWith() {
        RestAssured.get("/test/with")
                .then()
                .statusCode(200)
                .body(equalTo("with"))
                .header("Cache-Control", "no-store");
    }

    @Test
    public void testWithout() {
        RestAssured.get("/test/without")
                .then()
                .statusCode(200)
                .body(equalTo("without"))
                .header("Cache-Control", "no-cache, no-transform, proxy-revalidate, s-maxage=100");
    }

    @Path("test")
    @Cache(sMaxAge = 100, noTransform = true, proxyRevalidate = true, noCache = true)
    public static class ResourceWithCache {

        @Path("with")
        @Cache(noStore = true)
        @GET
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
