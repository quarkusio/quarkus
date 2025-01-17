package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.*;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class OptionalQueryParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testQueryParamsWithOptionals() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        String responseForEmptyOptional = client.optional(Optional.empty());
        assertThat(responseForEmptyOptional).isNullOrEmpty();

        String responseForValueOptional = client.optional(Optional.of("value"));
        assertThat(responseForValueOptional).isEqualTo("parameter=value");
    }

    @Path("optional")
    public static class Resource {

        @Path("query")
        @GET
        public String queryParams(@Context UriInfo uriInfo) {

            var entries = new ArrayList<String>(uriInfo.getQueryParameters().size());
            for (var entry : uriInfo.getQueryParameters().entrySet()) {
                entries.add(entry.getKey() + "=" + String.join(",", entry.getValue()));
            }
            return String.join(";", entries);

        }
    }

    @Path("optional")
    public interface Client {

        @Path("query")
        @GET
        String optional(@RestQuery Optional<String> parameter);

    }
}
