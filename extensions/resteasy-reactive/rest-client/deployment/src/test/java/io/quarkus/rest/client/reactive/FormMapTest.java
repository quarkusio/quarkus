package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormMapTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void test() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        String response = client.call(Map.of("foo", "bar", "k1", "v1", "k2", "v2"), new Holder(Collections.emptyMap()));
        assertThat(response).isEqualTo("foo=bar-k1=v1-k2=v2");

        String response2 = client.call(Collections.emptyMap(), new Holder(Map.of("foo", List.of("bar"), "k1", List.of("v1"))));
        assertThat(response2).isEqualTo("foo=bar-k1=v1");

        String response3 = client.call(Map.of("foo", "bar"), new Holder(Map.of("k", List.of("v1", "v2"))));
        assertThat(response3).isEqualTo("foo=bar-k=v1,v2");
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes("application/x-www-form-urlencoded")
        public String response(MultivaluedMap<String, String> all) {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
            List<String> keys = new ArrayList<>(all.keySet());
            Collections.sort(keys);
            for (var key : keys) {
                if (!isFirst) {
                    sb.append("-");
                }
                isFirst = false;
                sb.append(key).append("=").append(String.join(",", all.get(key)));
            }
            return sb.toString();
        }
    }

    @Path("/test")
    public interface Client {

        @POST
        String call(@RestForm Map<String, String> formParams, @BeanParam Holder holder);
    }

    public static class Holder {
        @RestForm
        public Map<String, List<String>> more;

        public Holder(Map<String, List<String>> more) {
            this.more = more;
        }
    }
}
