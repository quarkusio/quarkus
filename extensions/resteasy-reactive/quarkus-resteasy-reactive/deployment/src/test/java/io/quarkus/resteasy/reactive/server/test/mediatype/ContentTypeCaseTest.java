package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ContentTypeCaseTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class);
                }
            });

    @Test
    public void test() {
        given().header("test", "TeXt/Plain").get("/hello")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("text/plain"));

        given().header("test", "text/plain").get("/hello")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("text/plain"));

        given().header("test", "TEXT/PLAIN").get("/hello")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("text/plain"));
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public Response hello(@HeaderParam("test") String contentType) {
            MediaType mediaType = MediaType.valueOf(contentType);
            return Response.ok(mediaType.toString()).header("content-type", mediaType).build();
        }
    }
}
