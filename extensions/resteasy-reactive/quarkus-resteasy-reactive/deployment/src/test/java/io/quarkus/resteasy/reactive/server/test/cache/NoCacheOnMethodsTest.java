package io.quarkus.resteasy.reactive.server.test.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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

public class NoCacheOnMethodsTest {

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
    public static class ResourceWithNoCache {

        @Path("withFields")
        @GET
        @NoCache(fields = { "f1", "f2" })
        public String withFields() {
            return "withFields";
        }

        @Path("withoutFields")
        @GET
        @NoCache
        public String withoutFields() {
            return "withoutFields";
        }

        @Path("withoutAnnotation")
        @GET
        public String withoutAnnotation() {
            return "withoutAnnotation";
        }
    }
}
