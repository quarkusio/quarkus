package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientFormParam;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientFormParamFromPropertyTest {
    private static final String FORM_VALUE = "foo";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + FORM_VALUE),
                            "application.properties"));

    @Test
    void shouldSetFromProperties() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.getWithParam()).isEqualTo(FORM_VALUE);
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

        assertThat(client.missingNonRequiredProperty()).isEqualTo(FORM_VALUE);
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
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String returnFormParamValue(@FormParam("my-param") String param) {
            return param;
        }
    }

    @ClientFormParam(name = "my-param", value = "${my.property-value}")
    public interface Client {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String getWithParam();

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "some-other-param", value = "${non-existent-property}")
        String missingRequiredProperty();

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "some-other-param", value = "${non-existent-property}", required = false)
        String missingNonRequiredProperty();

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "some-other-param", value = "${non-existent-property}", required = false)
        @ClientFormParam(name = "my-param", value = "other")
        String missingNonRequiredPropertyAndOverriddenValue();
    }

}
