package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Blocking;

public class ClientResponseAsServerResponseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BackendResource.class, BackendClient.class, ProxyResource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.backend.url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @Test
    void testClientResponseReturnedDirectlyFromServerEndpointIsRejected() {
        when().get("/proxy/response")
                .then()
                .statusCode(500);
    }

    @Test
    void testClientRestResponseReturnedDirectlyFromServerEndpointIsRejected() {
        when().get("/proxy/rest-response")
                .then()
                .statusCode(500);
    }

    @Path("/backend")
    public static class BackendResource {

        @GET
        public String get() {
            return "backend-response";
        }
    }

    @Path("/backend")
    @RegisterRestClient(configKey = "backend")
    public interface BackendClient {

        @GET
        Response getResponse();

        @GET
        RestResponse<String> getRestResponse();
    }

    @Path("/proxy")
    public static class ProxyResource {

        private final BackendClient backendClient;

        public ProxyResource(@RestClient BackendClient backendClient) {
            this.backendClient = backendClient;
        }

        @GET
        @Path("response")
        @Blocking
        public Response proxyResponse() {
            return backendClient.getResponse();
        }

        @GET
        @Path("rest-response")
        @Blocking
        public RestResponse<String> proxyRestResponse() {
            return backendClient.getRestResponse();
        }
    }
}
