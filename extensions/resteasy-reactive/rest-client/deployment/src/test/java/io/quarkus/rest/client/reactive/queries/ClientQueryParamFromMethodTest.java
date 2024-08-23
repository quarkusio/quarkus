package io.quarkus.rest.client.reactive.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientQueryParamFromMethodTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, SubClient.class, Resource.class, ComputedParam.class));

    @Test
    void shouldUseValuesOnlyFromClass() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromClass()).isEqualTo("1/");
    }

    @Test
    void shouldUseValuesFromClassAndMethod() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromMethodAndClass()).isEqualTo("1/2");
    }

    @Test
    void shouldUseValuesFromMethodWithParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromMethodWithParam()).isEqualTo("-11/-2");
    }

    @Test
    void shouldUseValuesFromQueryParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromQueryParam("111")).isEqualTo("111/2");
    }

    @Test
    void shouldUseValuesFromQueryParams() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromQueryParams("111", "222")).isEqualTo("111/222");
    }

    @Test
    void shouldUseValuesFromSubclientAnnotations() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.sub().sub("22")).isEqualTo("11/22");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnQueryParamValues(@QueryParam("first") List<String> first,
                @QueryParam("second") List<String> second) {
            return String.join(",", first) + "/" + String.join(",", second);
        }
    }

    @ClientQueryParam(name = "first", value = "{first}")
    public interface Client {
        @GET
        String setFromClass();

        @GET
        @ClientQueryParam(name = "second", value = "{second}")
        String setFromMethodAndClass();

        @GET
        @ClientQueryParam(name = "second", value = "{second}")
        String setFromQueryParam(@QueryParam("first") String first);

        @GET
        @ClientQueryParam(name = "second", value = "{second}")
        String setFromQueryParams(@QueryParam("first") String first, @QueryParam("second") String second);

        @GET
        @ClientQueryParam(name = "first", value = "{io.quarkus.rest.client.reactive.queries.ComputedParam.withParam}")
        @ClientQueryParam(name = "second", value = "{withParam}")
        String setFromMethodWithParam();

        @Path("")
        SubClient sub();

        default String first() {
            return "1";
        }

        default String second() {
            return "2";
        }

        default String withParam(String name) {
            if ("first".equals(name)) {
                return "-1";
            } else if ("second".equals(name)) {
                return "-2";
            }
            throw new IllegalArgumentException();
        }
    }

    @ClientQueryParam(name = "first", value = "{first}")
    public interface SubClient {

        @GET
        String sub(@QueryParam("second") String second);

        default String first() {
            return "11";
        }
    }

}
