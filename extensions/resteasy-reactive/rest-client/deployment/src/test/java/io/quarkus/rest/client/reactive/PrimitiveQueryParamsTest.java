package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class PrimitiveQueryParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void testQueryParamsWithPrimitiveArrays() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        String first = client.query(new String[] {}, new int[] { 1 }, true);
        assertThat(first).isEqualTo("first=1;second=true");

        String second = client.query(new String[] { "foo", "bar" }, new int[] { 1, 2 });
        assertThat(second).isEqualTo("first=1,2;strings=foo,bar");
    }

    @Path("primitive")
    public static class Resource {

        @Path("query")
        @GET
        public String queryParams(@Context UriInfo uriInfo) {

            MultivaluedMap<String, String> queryParametersMap = uriInfo.getQueryParameters();
            var entries = new ArrayList<String>(queryParametersMap.size());
            List<String> queryParamNames = new ArrayList<>(queryParametersMap.keySet());
            Collections.sort(queryParamNames);
            for (var name : queryParamNames) {
                entries.add(name + "=" + String.join(",", queryParametersMap.get(name)));
            }
            return String.join(";", entries);

        }
    }

    @Path("primitive")
    public interface Client {

        @Path("query")
        @GET
        String query(@RestQuery String[] strings, @RestQuery int[] first, @RestQuery boolean... second);

    }
}
