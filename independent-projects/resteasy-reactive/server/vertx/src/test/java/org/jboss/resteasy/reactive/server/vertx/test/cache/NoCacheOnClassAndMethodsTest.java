package org.jboss.resteasy.reactive.server.vertx.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.NoCache;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.scanning.CacheControlScanner;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoCacheOnClassAndMethodsTest {

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
                            .addClasses(ResourceWithNoCache.class);
                }
            });

    @Test
    public void testWith() {
        RestAssured.get("/test/with")
                .then()
                .statusCode(200)
                .body(equalTo("with"))
                .header("Cache-Control", "no-cache=\"f1\", no-cache=\"f2\"");
    }

    @Test
    public void testWithout() {
        RestAssured.get("/test/without")
                .then()
                .statusCode(200)
                .body(equalTo("without"))
                .header("Cache-Control", "no-cache=\"f1\"");
    }

    @NoCache(fields = "f1")
    @Path("test")
    public static class ResourceWithNoCache {

        @Path("with")
        @GET
        @NoCache(fields = { "f1", "f2" })
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
