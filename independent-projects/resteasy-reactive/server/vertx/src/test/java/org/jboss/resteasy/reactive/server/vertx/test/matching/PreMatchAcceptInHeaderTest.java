package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PreMatchAcceptInHeaderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class);
                }
            });

    @Test
    void browserDefault() {
        given().accept("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(containsString("<html>"));
    }

    @Test
    void text() {
        given().accept("text/plain")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Test
    void html() {
        given().accept("text/html")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(containsString("<html>"));
    }

    @Test
    void json() {
        given().accept("application/json")
                .when()
                .get("test")
                .then()
                .statusCode(406);
    }

    @Test
    void setAcceptToTextInFilter() {
        given().accept("application/json")
                .header("x-set-accept-to-text", "true")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Test
    void entityJsonWithoutAcceptToTextInFilter() {
        given().accept("application/json")
                .when()
                .get("test/entity")
                .then()
                .statusCode(200)
                .body(containsString("\"text\""));
    }

    @Test
    void entityTextWithoutAcceptToTextInFilter() {
        given().accept("text/plain")
                .when()
                .get("test/entity")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Test
    void entityTextWithAcceptToTextInFilter() {
        given().accept("application/json")
                .header("x-set-accept-to-text", "true")
                .when()
                .get("test/entity")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Test
    void responseEntityJsonWithoutAcceptToTextInFilter() {
        given().accept("application/json")
                .when()
                .get("test/response")
                .then()
                .statusCode(200)
                .body(containsString("\"text\""));
    }

    @Test
    void responseEntityTextWithoutAcceptToTextInFilter() {
        given().accept("text/plain")
                .when()
                .get("test/response")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Test
    void responseEntityTextWithAcceptToTextInFilter() {
        given().accept("application/json")
                .header("x-set-accept-to-text", "true")
                .when()
                .get("test/response")
                .then()
                .statusCode(200)
                .body(equalTo("text"));
    }

    @Path("/test")
    public static class Resource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String text() {
            return "text";
        }

        @GET
        @Produces(MediaType.TEXT_HTML)
        public String html() {
            return """
                    <html>
                     <head>
                     </head>
                     <body>
                       Hello World
                     </body>
                    </html>
                    """;
        }

        @GET
        @Path("entity")
        @Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
        public Entity entity() {
            return new Entity("text");
        }

        @GET
        @Path("response")
        @Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
        public Response response() {
            return Response.ok(new Entity("text")).build();
        }
    }

    public record Entity(String value) {
    }

    @PreMatching
    @Provider
    public static class SetAcceptHeaderFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            if ("true".equals(headers.getFirst("x-set-accept-to-text"))) {
                headers.putSingle(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);
            }
        }
    }

    @Provider
    @Produces(MediaType.TEXT_PLAIN)
    public static class DummyTextMessageBodyWriter implements ServerMessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
                MediaType mediaType) {
            return Entity.class.equals(type);
        }

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            context.serverResponse().end(((Entity) o).value());
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.equals(type);
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should not be called");
        }
    }

    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class DummyJsonMessageBodyWriter implements ServerMessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
                MediaType mediaType) {
            return Entity.class.equals(type);
        }

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            context.serverResponse().end("{\"value\":\"" + ((Entity) o).value() + "\"}");
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Entity.class.equals(type);
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should not be called");
        }
    }
}
