package io.quarkus.resteasy.reactive.server.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoCacheOnClassAndMethodsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
