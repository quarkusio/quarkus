package org.jboss.resteasy.reactive.server.vertx.test.providers;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StreamingOutputTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class);
                }
            });

    @Test
    public void testWith() {
        get("/test")
                .then()
                .statusCode(200)
                .body(equalTo("hello world"));
    }

    @Test
    public void testWithResponse() {
        get("/test/response")
                .then()
                .statusCode(200)
                .body(equalTo("hello world"));
    }

    @Path("test")
    public static class TestResource {

        public static final byte[] HELLO_WORLD_BYTES = "hello world".getBytes(StandardCharsets.UTF_8);

        @GET
        public StreamingOutput with() {
            return new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    output.write(HELLO_WORLD_BYTES);
                }
            };
        }

        @GET
        @Path("response")
        public Response withResponse() {
            return Response.ok()
                    .entity(new StreamingOutput() {
                        @Override
                        public void write(OutputStream output) throws IOException, WebApplicationException {
                            output.write(HELLO_WORLD_BYTES);
                        }
                    })
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }
}
