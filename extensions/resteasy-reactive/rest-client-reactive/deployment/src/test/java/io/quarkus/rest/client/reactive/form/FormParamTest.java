package io.quarkus.rest.client.reactive.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormParamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FormClient.class, SubFormClient.class, Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldPassFormParam() {
        FormClient formClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(FormClient.class);
        String result = formClient.directForm("par1", "par2");
        assertThat(result).isEqualTo("root formParam1:par1,formParam2:par2");
    }

    @Test
    void shouldPassFormParamFromSubResource() {
        FormClient formClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(FormClient.class);
        String result = formClient.subForm("par1", "par2").form("spar1", "spar2");
        assertThat(result).isEqualTo("sub rootParam1:par1,rootParam2:par2,subParam1:spar1,subParam2:spar2");
    }

    public interface FormClient {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String directForm(@FormParam("formParam1") String formParam1, @FormParam("formParam2") String formParam2);

        @Path("/sub")
        SubFormClient subForm(@FormParam("rootParam1") String formParam1, @FormParam("rootParam2") String formParam2);
    }

    public interface SubFormClient {
        @PUT
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        String form(@FormParam("subParam1") String formParam1, @FormParam("subParam2") String formParam2);
    }
}
