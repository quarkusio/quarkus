package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.Separator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SeparatorFormParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void restFormWithSeparatorShouldSendSingleValue() {
        Client client = createClient();
        assertThat(client.restForm(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Test
    void formParamWithSeparatorShouldSendSingleValue() {
        Client client = createClient();
        assertThat(client.formParam(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Test
    void withoutSeparatorShouldSendMultiplePairs() {
        Client client = createClient();
        assertThat(client.plain(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS&frameworks=HELIDON");
    }

    public interface Client {
        @POST
        @Path("/separator/form")
        String restForm(@RestForm @Separator(",") List<String> frameworks);

        @POST
        @Path("/separator/form")
        String formParam(@FormParam("frameworks") @Separator(",") List<String> frameworks);

        @POST
        @Path("/separator/form")
        String plain(@RestForm List<String> frameworks);
    }

    Client createClient() {
        return RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
    }

    @Path("/separator")
    public static class Resource {

        @POST
        @Path("/form")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String form(String rawBody) {
            return rawBody;
        }
    }
}
