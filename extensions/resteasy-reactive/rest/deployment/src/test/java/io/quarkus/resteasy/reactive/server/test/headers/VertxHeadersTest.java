package io.quarkus.resteasy.reactive.server.test.headers;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.RouteFilter;
import io.restassured.http.Headers;
import io.vertx.ext.web.RoutingContext;

public class VertxHeadersTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(VertxFilter.class, JaxRsFilter.class, TestResource.class));

    @Test
    void testVaryHeaderValues() {
        var headers = when().get("/test")
                .then()
                .statusCode(200)
                .extract().headers();
        assertThat(headers.getValues(HttpHeaders.VARY)).containsExactlyInAnyOrder("Origin", "Prefer");
    }

    @Test
    void testTransferEncodingHeaderValues() {
        Headers headers = when().get("/test/response")
                .then()
                .statusCode(200)
                .header("Transfer-Encoding", is("chunked")).extract().headers();

        assertThat(headers.asList()).noneMatch(h -> h.getName().equals("transfer-encoding"));
    }

    public static class VertxFilter {
        @RouteFilter
        void addVary(final RoutingContext rc) {
            rc.response().headers().add(HttpHeaders.VARY, "Origin");
            rc.next();
        }
    }

    @Provider
    public static class JaxRsFilter implements ContainerResponseFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add(HttpHeaders.VARY, "Prefer");
        }
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String test() {
            return "test";
        }

        @GET
        @Path("response")
        public Response response() {
            final String text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.";
            TestDTO testDTO = new TestDTO();
            for (int i = 0; i < 100; i++) {
                testDTO.setText(testDTO.getText() + text);
                testDTO.setMoreText(testDTO.getMoreText() + text);
                testDTO.setEvenMoreText(testDTO.getEvenMoreText() + text);
            }

            //Simulate getting a chunked response from external microservice
            Response response = Response.ok().entity("test").header("Transfer-Encoding", "chunked").build();
            //Forwarding the response
            return Response.fromResponse(response).build();
        }
    }

    public static class TestDTO {
        public String text = "";
        public String moreText = "";
        public String evenMoreText = "";

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getMoreText() {
            return moreText;
        }

        public void setMoreText(String moreText) {
            this.moreText = moreText;
        }

        public String getEvenMoreText() {
            return evenMoreText;
        }

        public void setEvenMoreText(String evenMoreText) {
            this.evenMoreText = evenMoreText;
        }
    }
}
