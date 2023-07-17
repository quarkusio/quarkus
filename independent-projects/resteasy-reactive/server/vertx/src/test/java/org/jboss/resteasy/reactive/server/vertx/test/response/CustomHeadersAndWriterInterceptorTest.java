package org.jboss.resteasy.reactive.server.vertx.test.response;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Date;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.Headers;

public class CustomHeadersAndWriterInterceptorTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest runner = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, DummyWriterInterceptor.class));

    @Test
    void testResponseHeaders() {
        Headers headers = when()
                .get("/test")
                .then()
                .statusCode(200)
                .header("etag", is("0"))
                .extract().headers();
        assertThat(headers.getList("etag")).hasSize(1);
        assertThat(headers.getList("Last-Modified")).hasSize(1);
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response hello() {
            return Response.ok("123").lastModified(new Date()).header("etag", 0).build();
        }
    }

    @Provider
    public static class DummyWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.proceed();
        }
    }

}
