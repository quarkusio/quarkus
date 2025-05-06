package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.ContextLocals;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class ContextLocalPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.client.url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @Test
    void testQueryParamsWithPrimitiveArrays() {
        when().get("test/invokeClient")
                .then()
                .statusCode(200)
                .body(is("test/foo/bar"));
    }

    @Path("test")
    public static class Resource {

        private final Client client;

        public Resource(@RestClient Client client) {
            this.client = client;
        }

        @Path("invokeClient")
        @GET
        public String invokeClient() {
            var result = client.get();
            Optional<String> fromRequest = ContextLocals.get("fromRequest");
            Optional<String> fromResponse = ContextLocals.get("fromResponse");
            return result + "/" + fromRequest.orElse("none") + "/" + fromResponse.orElse("none");
        }

        @Path("toClient")
        @GET
        public String toClient() {
            return "test";
        }
    }

    @Path("test")
    @RegisterRestClient(configKey = "client")
    @RegisterProvider(RequestFilter.class)
    @RegisterProvider(ResponseFilter.class)
    public interface Client {

        @GET
        @Path("toClient")
        String get();
    }

    public static class RequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            RestClientContextUtil.putLocal("fromRequest", "foo");
        }
    }

    public static class ResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
                throws IOException {
            RestClientContextUtil.putLocal("fromResponse", "bar");
        }
    }

    public static final class RestClientContextUtil {

        private RestClientContextUtil() {
        }

        public static void putLocal(Object key, Object value) {
            determineRestClientContext().putLocal(key, value);
        }

        private static Context determineRestClientContext() {
            // In an ideal world, this would always be populated, however because we never
            // defined a proper execution model for the REST Client handlers, currently we are
            // in a situation where request filters could be run on the calling context
            // and not the client's purpose built context.
            // We will need a proper solution soon, but as we need to have a proper way to
            // set contextual information in Quarkus 3.20 (LTS), we can't risk breaking
            // client code everywhere, so for now we will tell people to check the context
            var maybeParentContext = ContextLocals.getParentContext();
            Context effectiveContext;
            if (maybeParentContext != null) {
                effectiveContext = maybeParentContext;
            } else {
                effectiveContext = Vertx.currentContext();
            }
            return effectiveContext;
        }
    }
}
