package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class MapParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testQueryParamsWithRegularMap() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        String response = client.regularMap(Map.of("foo", "bar", "k", "v"));
        assertThat(response).contains("foo=bar").contains("k=v");
    }

    @Test
    void testQueryParamsWithMultiMap() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        MultivaluedMap<String, Integer> map = new MultivaluedHashMap<>();
        map.putSingle("first", 1);
        map.put("second", List.of(2, 4, 6));
        String response = client.multiMap(map);
        assertThat(response).contains("first=1").contains("second=2,4,6");
    }

    @Path("map")
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

    @Path("map")
    public interface Client {

        @Path("query")
        @GET
        String regularMap(@RestQuery Map<String, String> queryParams);

        @Path("query")
        @GET
        String multiMap(@RestQuery MultivaluedMap<String, Integer> queryParams);
    }
}
