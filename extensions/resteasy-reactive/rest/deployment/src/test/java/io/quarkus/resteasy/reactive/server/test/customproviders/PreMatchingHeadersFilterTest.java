package io.quarkus.resteasy.reactive.server.test.customproviders;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PreMatchingHeadersFilterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    public void testJsonHeaderAdded() {
        given()
                .body("{\"foo\": \"bar\"}")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body("foo", equalTo("bar"));
    }

    @Path("test")
    public static class Resource {

        @POST
        @Consumes("application/json")
        @Produces("application/json")
        public String post(String json) {
            return json;
        }
    }

    public static class Filters {
        @ServerRequestFilter(preMatching = true)
        public void preMatchingFilter(ContainerRequestContext requestContext) {
            // without this, RR would respond with HTTP 415
            requestContext.getHeaders().put(HttpHeaders.CONTENT_TYPE, List.of(MediaType.APPLICATION_JSON));
        }
    }
}
