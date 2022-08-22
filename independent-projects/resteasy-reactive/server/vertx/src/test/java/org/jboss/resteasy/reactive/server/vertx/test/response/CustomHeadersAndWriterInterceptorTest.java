package org.jboss.resteasy.reactive.server.vertx.test.response;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;

import io.restassured.http.Headers;
import java.io.IOException;
import java.util.Date;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
