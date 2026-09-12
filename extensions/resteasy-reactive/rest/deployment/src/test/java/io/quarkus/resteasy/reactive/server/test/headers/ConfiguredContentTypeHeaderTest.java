package io.quarkus.resteasy.reactive.server.test.headers;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ConfiguredContentTypeHeaderTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class))
            .overrideRuntimeConfigKey("quarkus.http.header.\"Content-Type\".value", "text/html");

    @Test
    void responseWithoutMediaTypeUsesProduces() {
        assertThat(contentTypes("/configured-header/response")).containsExactly("text/plain;charset=UTF-8");
    }

    @Test
    void entityReturnUsesProduces() {
        assertThat(contentTypes("/configured-header/plain")).containsExactly("text/plain;charset=UTF-8");
    }

    @Test
    void responseWithMediaTypeKeepsIt() {
        assertThat(contentTypes("/configured-header/response-with-type")).containsExactly("application/json");
    }

    private static java.util.List<String> contentTypes(String path) {
        return when().get(path).then().statusCode(200).extract().headers().getValues("Content-Type");
    }

    @Path("/configured-header")
    public static class Resource {

        @GET
        @Path("response")
        @Produces(MediaType.TEXT_PLAIN)
        public Response response() {
            return Response.ok("hello").build();
        }

        @GET
        @Path("plain")
        @Produces(MediaType.TEXT_PLAIN)
        public String plain() {
            return "hello";
        }

        @GET
        @Path("response-with-type")
        @Produces(MediaType.TEXT_PLAIN)
        public Response responseWithType() {
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }
}
