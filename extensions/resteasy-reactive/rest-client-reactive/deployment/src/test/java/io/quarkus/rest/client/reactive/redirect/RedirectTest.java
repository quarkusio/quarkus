package io.quarkus.rest.client.reactive.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RedirectTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RedirectingResourceClient.class, RedirectingResourceWithCustomRedirectHandlerClient.class,
                            EnablePostRedirectHandler.class, RedirectingResource.class));

    @TestHTTPResource
    URI uri;

    @Test
    void shouldRedirect3Times_whenMax4() {
        RedirectingResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .property(QuarkusRestClientProperties.MAX_REDIRECTS, 4)
                .build(RedirectingResourceClient.class);
        Response call = client.call(3);
        assertThat(call.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotRedirect3Times_whenMax2() {
        RedirectingResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .property(QuarkusRestClientProperties.MAX_REDIRECTS, 2)
                .build(RedirectingResourceClient.class);
        assertThat(client.call(3).getStatus()).isEqualTo(307);
    }

    @Test
    void shouldNotRedirectOnPostMethodsByDefault() {
        RedirectingResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceClient.class);
        assertThat(client.post().getStatus()).isEqualTo(307);
    }

    @Test
    void shouldRedirectWhenUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                // use custom redirect to enable redirection
                .register(EnablePostRedirectHandler.class)
                .build(RedirectingResourceClient.class);
        assertThat(client.post().getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRedirectWhenAnnotatedUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceWithCustomRedirectHandlerClient.class);
        assertThat(client.post().getStatus()).isEqualTo(200);
    }
}
