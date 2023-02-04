package io.quarkus.rest.client.reactive.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class QueryTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testQuery() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQuery("bar")).isEqualTo("bar");
    }

    @Test
    void testNullQuery() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQuery(null)).isNull();
    }

    @Test
    void testQueryMap() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryMap(Map.of("a", "1", "b", "2"))).isEqualTo("a=1,b=2");
    }

    @Test
    void testNullQueryMap() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryMap(null)).isEqualTo("none");
    }

    @Test
    void testQueryCollection() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryCollection(List.of("a", "b"))).isEqualTo("a,b");
    }

    @Test
    void testNullQueryCollection() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryCollection(null)).isEqualTo("none");
    }

    @Test
    void testQueryArray() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryArray(new String[] { "a", "b" })).isEqualTo("a,b");
    }

    @Test
    void testNullQueryArray() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.sendQueryArray(null)).isEqualTo("none");
    }

    @Test
    void testQueriesWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.querySub("bar", "bar2").send("bar3", "bar4")).isEqualTo("bar:bar2:bar3:bar4");
    }

    @Test
    void testNullQueriesWithSubresource() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.querySub("bar", null).send(null, "bar4")).isEqualTo("bar:null:null:bar4");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnQueryValue(@QueryParam("foo") String query) {
            return query;
        }

        @Path("map")
        @GET
        public String returnQueryMap(@Context UriInfo uriInfo) {
            if (uriInfo.getQueryParameters().isEmpty()) {
                return "none";
            }
            return uriInfo.getQueryParameters().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(value -> new AbstractMap.SimpleEntry(entry.getKey(), value)))
                    .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        @Path("collection")
        @GET
        public String returnQueryCollection(@QueryParam("foo") List<String> query) {
            if (query.isEmpty()) {
                return "none";
            }
            return String.join(",", query);
        }

        @Path("array")
        @GET
        public String returnQueryArray(@QueryParam("foo") String[] query) {
            if (query.length == 0) {
                return "none";
            }
            return String.join(",", query);
        }

        @Path("2")
        @GET
        public String returnQueryValue2(@QueryParam("foo") String query, @QueryParam("foo2") String query2,
                @QueryParam("foo3") String query3, @QueryParam("foo4") String query4) {
            return query + ":" + query2 + ":" + query3 + ":" + query4;
        }
    }

    public interface Client {

        @GET
        String sendQuery(@QueryParam("foo") String query);

        @Path("map")
        @GET
        String sendQueryMap(@RestQuery Map<String, String> query);

        @Path("collection")
        @GET
        String sendQueryCollection(@QueryParam("foo") List<String> query);

        @Path("array")
        @GET
        String sendQueryArray(@QueryParam("foo") String[] query);

        @Path("2")
        SubClient querySub(@QueryParam("foo") String query, @QueryParam("foo2") String query2);
    }

    public interface SubClient {

        @GET
        String send(@QueryParam("foo3") String query3, @QueryParam("foo4") String query4);
    }

}
