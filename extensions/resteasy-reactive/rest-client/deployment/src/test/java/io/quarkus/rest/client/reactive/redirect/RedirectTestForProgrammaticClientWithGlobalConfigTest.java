package io.quarkus.rest.client.reactive.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RedirectTestForProgrammaticClientWithGlobalConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, RedirectingResource.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.follow-redirects", "true");

    @TestHTTPResource
    URI uri;

    @Test
    void shouldRedirect() {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        Response call = client.call(3);
        assertThat(call.getStatus()).isEqualTo(200);
    }

    @RegisterRestClient(configKey = "cl")
    @Path("/redirect/302")
    public interface Client {
        @GET
        Response call(@QueryParam("redirects") Integer redirects);
    }
}
