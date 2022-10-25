package io.quarkus.resteasy.reactive.server.test.response;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.providers.serialisers.ServerStringMessageBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ChunkedResponseTest {

    private static final String LARGE_HELLO_STRING = "h" + "e".repeat(256) + "llo";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class)
                    .addAsResource(new StringAsset("quarkus.rest.output-buffer-size = 256"),
                            "application.properties"));

    @Test
    void chunked() {
        when()
                .get("/hello/big")
                .then().statusCode(200)
                .body(equalTo(LARGE_HELLO_STRING))
                .header("Transfer-encoding", "chunked");
    }

    @Test
    void notChunked() {
        when()
                .get("/hello/small")
                .then().statusCode(200)
                .body(equalTo("hello"))
                .header("Transfer-encoding", nullValue());
    }

    @Path("hello")
    public static final class HelloResource {

        @GET
        @Path("big")
        public String helloBig() {
            return LARGE_HELLO_STRING;
        }

        @GET
        @Path("small")
        public String helloSmall() {
            return "hello";
        }
    }

    @Provider
    public static final class CustomStringMessageBodyWriter extends ServerStringMessageBodyHandler {

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException {

            try (OutputStream stream = context.getOrCreateOutputStream()) {
                stream.write(((String) o).getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
