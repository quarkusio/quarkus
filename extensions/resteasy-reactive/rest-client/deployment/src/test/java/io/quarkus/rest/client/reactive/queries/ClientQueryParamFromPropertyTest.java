package io.quarkus.rest.client.reactive.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientQueryParamFromPropertyTest {
    private static final String QUERY_VALUE = "foo";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + QUERY_VALUE),
                            "application.properties"));

    @Test
    void shouldSetFromProperties() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.getWithParam()).isEqualTo(QUERY_VALUE);
    }

    @Test
    void shouldFailOnMissingRequiredProperty() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThatThrownBy(client::missingRequiredProperty)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSucceedOnMissingNonRequiredProperty() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.missingNonRequiredProperty()).isEqualTo(QUERY_VALUE);
    }

    @Test
    void shouldSucceedOnMissingNonRequiredPropertyAndUseOverriddenValue() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.missingNonRequiredPropertyAndOverriddenValue()).isEqualTo("other");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnQueryParamValue(@QueryParam("my-param") String param) {
            return param;
        }
    }

    @ClientQueryParam(name = "my-param", value = "${my.property-value}")
    public interface Client {
        @GET
        String getWithParam();

        @GET
        @ClientQueryParam(name = "some-other-param", value = "${non-existent-property}")
        String missingRequiredProperty();

        @GET
        @ClientQueryParam(name = "some-other-param", value = "${non-existent-property}", required = false)
        String missingNonRequiredProperty();

        @GET
        @ClientQueryParam(name = "some-other-param", value = "${non-existent-property}", required = false)
        @ClientQueryParam(name = "my-param", value = "other")
        String missingNonRequiredPropertyAndOverriddenValue();
    }

}
