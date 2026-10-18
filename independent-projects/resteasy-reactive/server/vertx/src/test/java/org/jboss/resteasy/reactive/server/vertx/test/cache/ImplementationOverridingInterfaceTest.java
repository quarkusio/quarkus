package org.jboss.resteasy.reactive.server.vertx.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.processor.scanning.CacheControlScanner;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class ImplementationOverridingInterfaceTest {

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
                            .addClasses(IResourceWithCache.class, ResourceWithCache.class);
                }
            });

    @Test
    public void testWith() {
        // expect method annotation on implementation to override class annotation on interface
        RestAssured.get("/test/with")
                .then()
                .statusCode(200)
                .body(equalTo("with"))
                .header("Cache-Control", "no-store, max-age=100, private");
    }

    @Test
    public void testWithout() {
        // expect class annotation on interface
        RestAssured.get("/test/without")
                .then()
                .statusCode(200)
                .body(equalTo("without"))
                .header("Cache-Control", "no-cache");
    }

    @Test
    public void testOverridden() {
        // expect method annotation on implementation to override method/class annotations on interface
        RestAssured.get("/test/overridden")
                .then()
                .statusCode(200)
                .body(equalTo("overridden"))
                .header("Cache-Control", "max-age=50");
    }

    @Test
    public void testUnoverridden() {
        // expect method annotation on default implementation in interface
        RestAssured.get("/test/unoverridden")
                .then()
                .statusCode(200)
                .body(equalTo("unoverridden"))
                .header("Cache-Control", "no-store");
    }

    @Path("test")
    @Cache(noCache = true)
    public interface IResourceWithCache {

        @Path("with")
        @GET
        String with();

        @Path("overridden")
        @GET
        @Cache(maxAge = 100, noStore = true, mustRevalidate = true, isPrivate = true)
        String overridden();

        @Path("without")
        @GET
        String without();

        @Path("unoverridden")
        @GET
        @Cache(noStore = true)
        default String unoverridden() {
            return "unoverridden";
        }
    }

    public static class ResourceWithCache implements IResourceWithCache {

        @Override
        @Cache(maxAge = 100, noStore = true, isPrivate = true)
        public String with() {
            return "with";
        }

        @Override
        @Cache(maxAge = 50)
        public String overridden() {
            return "overridden";
        }

        @Override
        public String without() {
            return "without";
        }
    }
}
