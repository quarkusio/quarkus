package io.quarkus.rest.client.reactive.headers;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class ReactiveClientHeadersFromProviderTest {
    private static final String HEADER_NAME = "my-header";
    private static final String HEADER_VALUE = "oifajrofijaeoir5gjaoasfaxcvcz";
    public static final String COPIED_INCOMING_HEADER = "copied-incoming-header";
    public static final String INCOMING_HEADER = "incoming-header";
    public static final String DIRECT_HEADER_PARAM = "direct-header-param";
    public static final String DIRECT_HEADER_PARAM_VAL = "direct-header-param-val";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(ReactiveClientHeadersFromProviderTest.Client.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + HEADER_VALUE),
                            "application.properties"));

    @Test
    void shouldPropagateHeaders() {
        // we're calling a resource that sets "incoming-header" header
        // this header should be dropped by the client and its value should be put into copied-incoming-header
        String propagatedHeaderValue = "propag8ed header";
        // @formatter:off
        var response =
                given()
                   .header(INCOMING_HEADER, propagatedHeaderValue)
                   .body(baseUri.toString())
                .when()
                   .post("/call-client")
                .thenReturn();
        // @formatter:on
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString(INCOMING_HEADER)).isNull();
        assertThat(response.jsonPath().getString(COPIED_INCOMING_HEADER)).isEqualTo(format("[%s]", propagatedHeaderValue));
        assertThat(response.jsonPath().getString(HEADER_NAME)).isEqualTo(format("[%s]", HEADER_VALUE));
        assertThat(response.jsonPath().getString(DIRECT_HEADER_PARAM)).isEqualTo(format("[%s]", DIRECT_HEADER_PARAM_VAL));
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {

        @GET
        @Produces("application/json")
        public Map<String, List<String>> returnHeaderValues(@Context HttpHeaders headers) {
            return headers.getRequestHeaders();
        }

        @Path("/call-client")
        @POST
        public Map<String, List<String>> callClient(String uri) {
            ReactiveClientHeadersFromProviderTest.Client client = RestClientBuilder.newBuilder().baseUri(URI.create(uri))
                    .build(ReactiveClientHeadersFromProviderTest.Client.class);
            return client.getWithHeader(DIRECT_HEADER_PARAM_VAL);
        }
    }

    @ApplicationScoped
    public static class Service {
        @Blocking
        public String getValue() {
            return HEADER_VALUE;
        }
    }

    @RegisterClientHeaders(CustomReactiveClientHeadersFactory.class)
    public interface Client {
        @GET
        Map<String, List<String>> getWithHeader(@HeaderParam(DIRECT_HEADER_PARAM) String directHeaderParam);
    }

    public static class CustomReactiveClientHeadersFactory extends ReactiveClientHeadersFactory {

        @Inject
        Service service;

        @Inject
        Vertx vertx;

        @Override
        public Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders,
                MultivaluedMap<String, String> clientOutgoingHeaders) {
            return Uni.createFrom().emitter(e -> {
                MultivaluedHashMap<String, String> newHeaders = new MultivaluedHashMap<>();
                newHeaders.add(HEADER_NAME, service.getValue());
                newHeaders.add(COPIED_INCOMING_HEADER, incomingHeaders.getFirst(INCOMING_HEADER));
                vertx.setTimer(100L, id -> e.complete(newHeaders));
            });
        }
    }
}
