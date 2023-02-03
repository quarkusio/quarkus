package io.quarkus.jaxrs.client.reactive.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ClientRequestFilterAbortWithTestCase {

    private static final int ABORT_WITH_STATUS = 999;
    private static final String ABORT_WITH_REASON_PHRASE = "ABORTED";
    private static final String ABORT_WITH_ENTITY = "ABORT_ENTITY";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class));

    @TestHTTPResource
    URL url;

    private Client client;

    @BeforeEach
    public void before() {
        client = ClientBuilder.newClient().register(TestClientRequestFilter.class);
    }

    @AfterEach
    public void after() {
        client.close();
    }

    @Test
    public void test() {
        // test with specified response type
        // a non-null response type will ensure that RestClientRequestContext.checkSuccessfulFamily is true
        WebApplicationException thrown = Assertions.assertThrows(WebApplicationException.class,
                () -> client.target(targetUrl()).request().get(String.class));

        assertResponseStatusFor(thrown.getResponse());

        // test with unspecified response type
        Response response = client.target(targetUrl()).request().get();
        assertResponseStatusFor(response);
        // entity only propagated with unspecified response type
        assertEquals(ABORT_WITH_ENTITY, response.readEntity(String.class));
    }

    private String targetUrl() {
        return url.toExternalForm() + "/hello";
    }

    private void assertResponseStatusFor(Response response) {
        assertEquals(ABORT_WITH_STATUS, response.getStatus());
        assertEquals(ABORT_WITH_REASON_PHRASE, response.getStatusInfo().getReasonPhrase());
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/hello").handler(new Handler<>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().setStatusCode(200).end();
                }
            });
        }
    }

    @Provider
    public static class TestClientRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext
                    .abortWith(Response.status(ABORT_WITH_STATUS, ABORT_WITH_REASON_PHRASE).entity(ABORT_WITH_ENTITY).build());
        }
    }
}
