package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientFormParam;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ClientFormParamFromMethodTest {

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
    void shouldUseValuesFromFormParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromFormParam("111")).isEqualTo("111/2");
    }

    @Test
    void shouldUseValuesFromFormParams() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(Client.class);

        assertThat(client.setFromFormParams("111", "222")).isEqualTo("111/222");
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
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String returnFormParamValues(@FormParam("first") List<String> first,
                @FormParam("second") List<String> second) {
            return String.join(",", first) + "/" + String.join(",", second);
        }
    }

    @ClientFormParam(name = "first", value = "{first}")
    public interface Client {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String setFromClass();

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "second", value = "{second}")
        String setFromMethodAndClass();

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "second", value = "{second}")
        String setFromFormParam(@FormParam("first") String first);

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "second", value = "{second}")
        String setFromFormParams(@FormParam("first") String first, @FormParam("second") String second);

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        @ClientFormParam(name = "first", value = "{io.quarkus.rest.client.reactive.form.ComputedParam.withParam}")
        @ClientFormParam(name = "second", value = "{withParam}")
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

    @ClientFormParam(name = "first", value = "{first}")
    public interface SubClient {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String sub(@FormParam("second") String second);

        default String first() {
            return "11";
        }
    }

}
