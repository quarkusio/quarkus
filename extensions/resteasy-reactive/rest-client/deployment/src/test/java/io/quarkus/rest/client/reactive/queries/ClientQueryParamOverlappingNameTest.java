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
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientQueryParamOverlappingNameTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @Test
    void shouldSendParamWhoseNameEndsAnotherParamName() {
        assertThat(client().pageAndPerPage()).isEqualTo("page=[2] per_page=[1]");
    }

    @Test
    void shouldPreferMethodQueryParamWithTheSameName() {
        assertThat(client().pageFromMethod("3")).isEqualTo("page=[3] per_page=[1]");
    }

    @Test
    void shouldSendParamWhoseNameIsEncoded() {
        assertThat(client().encodedNames()).isEqualTo("filter[name]=[a] sort by=[b]");
    }

    @Test
    void shouldPreferMethodQueryParamWhoseNameIsPercentEncoded() {
        assertThat(client().bracketsFromMethod("from-method")).isEqualTo("filter[name]=[from-method] sort by=[b]");
    }

    @Test
    void shouldPreferMethodQueryParamWhoseNameHasASpace() {
        assertThat(client().spaceFromMethod("from-method")).isEqualTo("filter[name]=[a] sort by=[from-method]");
    }

    @Test
    void shouldPreferMethodQueryParamWhoseNameContainsAPercentSequence() {
        assertThat(client().percentFromMethod("from-method")).isEqualTo("a%20b=[from-method]");
    }

    private Client client() {
        return RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        @Path("paging")
        public String paging(@QueryParam("page") List<String> page, @QueryParam("per_page") List<String> perPage) {
            return "page=" + page + " per_page=" + perPage;
        }

        @GET
        @Path("encoded")
        public String encoded(@QueryParam("filter[name]") List<String> filter, @QueryParam("sort by") List<String> sort) {
            return "filter[name]=" + filter + " sort by=" + sort;
        }

        @GET
        @Path("percent")
        public String percent(@QueryParam("a%20b") List<String> value) {
            return "a%20b=" + value;
        }
    }

    public interface Client {
        @GET
        @Path("paging")
        @ClientQueryParam(name = "per_page", value = "1")
        @ClientQueryParam(name = "page", value = "2")
        String pageAndPerPage();

        @GET
        @Path("paging")
        @ClientQueryParam(name = "per_page", value = "1")
        @ClientQueryParam(name = "page", value = "2")
        String pageFromMethod(@QueryParam("page") String page);

        @GET
        @Path("encoded")
        @ClientQueryParam(name = "filter[name]", value = "a")
        @ClientQueryParam(name = "sort by", value = "b")
        String encodedNames();

        @GET
        @Path("encoded")
        @ClientQueryParam(name = "filter[name]", value = "from-annotation")
        @ClientQueryParam(name = "sort by", value = "b")
        String bracketsFromMethod(@QueryParam("filter[name]") String filter);

        @GET
        @Path("encoded")
        @ClientQueryParam(name = "filter[name]", value = "a")
        @ClientQueryParam(name = "sort by", value = "from-annotation")
        String spaceFromMethod(@QueryParam("sort by") String sort);

        @GET
        @Path("percent")
        @ClientQueryParam(name = "a%20b", value = "from-annotation")
        String percentFromMethod(@QueryParam("a%20b") String value);
    }
}
