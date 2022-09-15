package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ContextResolver;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.http.HttpClientOptions;

public class CustomHttpOptionsViaProgrammaticallyClientCreatedTest {

    private static final String EXPECTED_VALUE = "success";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest app = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class));

    @Test
    void shouldUseCustomHttpOptions() {
        // First verify the standard configuration
        assertThat(RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class).get())
                .isEqualTo(EXPECTED_VALUE);

        // Now, it should fail if we use a custom http client options with a very limited max header size:

        Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .register(CustomHttpClientOptionsWithLimit.class)
                .build(Client.class);
        assertThatThrownBy(() -> client.get()).hasMessageContaining("HTTP header is larger than 1 bytes.");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public RestResponse<String> get() {
            return RestResponse.ResponseBuilder.ok(EXPECTED_VALUE)
                    .header("long-header", "VERY LONNGGGGGGGGGGGGGGGGGGGGGGGGGGGG!")
                    .build();
        }
    }

    public interface Client {
        @GET
        String get();
    }

    public static class CustomHttpClientOptionsWithLimit implements ContextResolver<HttpClientOptions> {

        @Override
        public HttpClientOptions getContext(Class<?> aClass) {
            HttpClientOptions options = new HttpClientOptions();
            options.setMaxHeaderSize(1); // this is just to verify that this HttpClientOptions is indeed used.
            return options;
        }
    }
}
