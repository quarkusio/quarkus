package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientHeaderParamFromPropertyTest {
    private static final String HEADER_VALUE = "oifajrofijaeoir5gjaoasfaxcvcz";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + HEADER_VALUE),
                            "application.properties"));

    @Test
    void shouldSetHeaderFromProperties() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.getWithHeader()).isEqualTo(HEADER_VALUE);
    }

    @Test
    void shouldFailOnMissingRequiredHeaderProperty() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThatThrownBy(client::missingRequiredProperty)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSucceedOnMissingNonRequiredHeaderProperty() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.missingNonRequiredProperty()).isEqualTo(HEADER_VALUE);
    }

    @Test
    void shouldSupportAdditionalTextOnHeaderProperty() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.supportAdditionalText()).isEqualTo("Bearer " + HEADER_VALUE + " Token");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaderValue(@HeaderParam("my-header") String header) {
            return header;
        }

        @GET
        @Path("/additional-text")
        public String returnAdditionalText(@HeaderParam("additional-text") String header) {
            return header;
        }

    }

    @ClientHeaderParam(name = "my-header", value = "${my.property-value}")
    public interface Client {
        @GET
        String getWithHeader();

        @GET
        @ClientHeaderParam(name = "some-other-header", value = "${non-existent-property}")
        String missingRequiredProperty();

        @GET
        @ClientHeaderParam(name = "some-other-header", value = "${non-existent-property}", required = false)
        String missingNonRequiredProperty();

        @GET
        @ClientHeaderParam(name = "additional-text", value = "Bearer ${my.property-value} Token")
        @Path("/additional-text")
        String supportAdditionalText();

    }
}
