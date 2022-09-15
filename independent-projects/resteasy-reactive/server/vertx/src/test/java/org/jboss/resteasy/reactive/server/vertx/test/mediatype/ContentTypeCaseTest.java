package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ContentTypeCaseTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
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
