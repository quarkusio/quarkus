package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.Separator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ParamStyle;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormParamStyleTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class, ConfiguredClient.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.configured-client.url",
                    "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.configured-client.form-param-style", "COMMA_SEPARATED");

    @TestHTTPResource
    URI baseUri;

    @RestClient
    ConfiguredClient configuredClient;

    @Test
    void defaultIsMultiPairs() {
        Client client = createClient(null);
        assertThat(client.list(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS&frameworks=HELIDON");
    }

    @Test
    void commaSeparated() {
        Client client = createClient(ParamStyle.COMMA_SEPARATED);
        assertThat(client.list(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
        assertThat(client.list(List.of("QUARKUS"))).isEqualTo("frameworks=QUARKUS");
    }

    @Test
    void arrayPairs() {
        Client client = createClient(ParamStyle.ARRAY_PAIRS);
        assertThat(client.list(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks[]=QUARKUS&frameworks[]=HELIDON");
        assertThat(client.list(List.of("QUARKUS"))).isEqualTo("frameworks=QUARKUS");
    }

    @Test
    void styleAppliesToMapValuesAndMixedParams() {
        Client client = createClient(ParamStyle.COMMA_SEPARATED);
        // the order of the form parameters is not guaranteed
        assertThat(client.mixed(List.of("QUARKUS", "HELIDON"), Map.of("versions", List.of("21", "25")), "JVM").split("&"))
                .containsExactlyInAnyOrder("frameworks=QUARKUS%2CHELIDON", "versions=21%2C25", "mode=JVM");
    }

    @Test
    void separatorTakesPrecedenceOverStyle() {
        Client client = createClient(ParamStyle.ARRAY_PAIRS);
        assertThat(client.separator(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%3BHELIDON");
    }

    @Test
    void styleAppliesToFormEntity() {
        Client client = createClient(ParamStyle.COMMA_SEPARATED);
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.addAll("frameworks", "QUARKUS", "HELIDON");
        assertThat(client.entity(form)).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Test
    void styleFromConfiguration() {
        assertThat(configuredClient.list(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Path("/style")
    public interface Client {
        @POST
        @Path("/form")
        String list(@RestForm List<String> frameworks);

        @POST
        @Path("/form")
        String mixed(@RestForm List<String> frameworks, @RestForm Map<String, List<String>> map, @RestForm String mode);

        @POST
        @Path("/form")
        String separator(@RestForm @Separator(";") List<String> frameworks);

        @POST
        @Path("/form")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        String entity(MultivaluedMap<String, String> form);
    }

    @Path("/style")
    @RegisterRestClient(configKey = "configured-client")
    public interface ConfiguredClient {
        @POST
        @Path("/form")
        String list(@RestForm List<String> frameworks);
    }

    Client createClient(ParamStyle style) {
        QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri);
        if (style != null) {
            builder.formParamStyle(style);
        }
        return builder.build(Client.class);
    }

    @Path("/style")
    public static class Resource {

        @POST
        @Path("/form")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String form(String rawBody) {
            return rawBody;
        }
    }
}
