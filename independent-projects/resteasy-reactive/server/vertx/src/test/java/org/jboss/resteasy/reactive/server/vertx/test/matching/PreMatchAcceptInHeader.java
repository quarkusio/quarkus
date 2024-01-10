package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PreMatchAcceptInHeader {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(PathSegmentTest.Resource.class);
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
                .body(equalTo("test"));
    }

    @Test
    void html() {
        given().accept("text/html")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(equalTo("test"));
    }

    @Test
    void json() {
        given().accept("application/json")
                .when()
                .get("test")
                .then()
                .statusCode(404);
    }

    @Test
    void setAcceptToTextInFilter() {
        given().accept("application/json")
                .header("x-set-accept-to-text", "true")
                .when()
                .get("test")
                .then()
                .statusCode(200)
                .body(equalTo("test"));
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
}
