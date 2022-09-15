package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MediaTypeSuffixTest {

    private static final String CUSTOM_JSON_MEDIA_TYPE = "application/vnd.search.v1+json";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, Client.class))
            .withConfigurationResource("media-type-suffix-application.properties");

    @Test
    public void test() {
        when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body("foo", is("bar"));
    }

    @RegisterRestClient(configKey = "test")
    @Path("/hello")
    public interface Client {

        @GET
        @Path("/custom")
        @Produces(CUSTOM_JSON_MEDIA_TYPE)
        Map<String, Object> test();
    }

    @Path("/hello")
    public static class HelloResource {

        private final Client client;

        public HelloResource(@RestClient Client client) {
            this.client = client;
        }

        @GET
        @Path("/custom")
        @Produces(CUSTOM_JSON_MEDIA_TYPE)
        public Map<String, Object> hello() {
            return Map.of("foo", "bar");
        }

        @GET
        public Map<String, Object> test() {
            return client.test();
        }
    }
}
