package io.quarkus.resteasy.reactive.server.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.Cache;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CacheOnMethodsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
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
