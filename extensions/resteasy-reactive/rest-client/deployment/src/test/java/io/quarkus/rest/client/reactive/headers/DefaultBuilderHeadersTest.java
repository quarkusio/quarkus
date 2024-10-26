package io.quarkus.rest.client.reactive.headers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class DefaultBuilderHeadersTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> {
    });

    @TestHTTPResource
    URI baseUri;

    @Test
    void headers() {
        RestClientBuilder builder = RestClientBuilder.newBuilder().baseUri("http://localhost:8080/");
        builder.register(ReturnWithAllDuplicateClientHeadersFilter.class);
        builder.header("InterfaceAndBuilderHeader", "builder");
        ClientBuilderHeaderMethodClient client = builder.build(ClientBuilderHeaderMethodClient.class);

        checkHeaders(client.getAllHeaders("headerparam"), "method");
    }

    @Path("/")
    public interface ClientBuilderHeaderMethodClient {
        @GET
        @ClientHeaderParam(name = "InterfaceAndBuilderHeader", value = "method")
        JsonObject getAllHeaders(@HeaderParam("HeaderParam") String param);
    }

    public static class ReturnWithAllDuplicateClientHeadersFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            JsonObjectBuilder allClientHeaders = Json.createObjectBuilder();
            MultivaluedMap<String, Object> clientHeaders = clientRequestContext.getHeaders();
            for (String headerName : clientHeaders.keySet()) {
                List<Object> header = clientHeaders.get(headerName);
                final JsonArrayBuilder headerValues = Json.createArrayBuilder();
                header.forEach(h -> headerValues.add(h.toString()));
                allClientHeaders.add(headerName, headerValues);
            }
            clientRequestContext.abortWith(Response.ok(allClientHeaders.build()).build());
        }
    }

    private static void checkHeaders(final JsonObject headers, final String clientHeaderParamName) {
        final List<String> clientRequestHeaders = headerValues(headers, "InterfaceAndBuilderHeader");

        assertTrue(clientRequestHeaders.contains("builder"),
                "Header InterfaceAndBuilderHeader did not container \"builder\": " + clientRequestHeaders);
        assertTrue(clientRequestHeaders.contains(clientHeaderParamName),
                "Header InterfaceAndBuilderHeader did not container \"" + clientHeaderParamName + "\": "
                        + clientRequestHeaders);

        final List<String> headerParamHeaders = headerValues(headers, "HeaderParam");
        assertTrue(headerParamHeaders.contains("headerparam"),
                "Header HeaderParam did not container \"headerparam\": " + headerParamHeaders);
    }

    private static List<String> headerValues(final JsonObject headers, final String headerName) {
        final JsonArray headerValues = headers.getJsonArray(headerName);
        assertNotNull(headerValues,
                String.format("Expected header '%s' to be present in %s", headerName, headers));
        return headerValues.stream().map(
                v -> (v.getValueType() == JsonValue.ValueType.STRING ? ((JsonString) v).getString() : v.toString()))
                .collect(Collectors.toList());
    }
}
