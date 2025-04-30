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

public class AdvancedRedirectTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RedirectingResourceClient302.class,
                            RedirectingResourceWithRegisterProviderAdvancedRedirectHandlerClient.class,
                            EnablePostAdvancedRedirectHandler.class,
                            RedirectingResource.class));

    @TestHTTPResource
    URI uri;

    @Test
    void shouldRedirect3Times_whenMax4() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .property(QuarkusRestClientProperties.MAX_REDIRECTS, 4)
                .build(RedirectingResourceClient302.class);
        Response call = client.call(3);
        assertThat(call.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotRedirect3Times_whenMax2() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .property(QuarkusRestClientProperties.MAX_REDIRECTS, 2)
                .build(RedirectingResourceClient302.class);
        assertThat(client.call(3).getStatus()).isEqualTo(302);
    }

    @Test
    void shouldNotRedirectOnPostMethodsByDefault() {
        RedirectingResourceClient302 client302 = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceClient302.class);
        assertThat(client302.post().getStatus()).isEqualTo(302);
    }

    @Test
    void shouldRedirectWhenUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient302 client302 = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                // use custom redirect to enable redirection
                .register(EnablePostAdvancedRedirectHandler.class)
                .build(RedirectingResourceClient302.class);
        Response response = client302.post();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("x-foo")).isEqualTo("bar");
    }

    @Test
    void shouldRedirectWhenRegisterProviderUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceWithRegisterProviderAdvancedRedirectHandlerClient.class);
        Response response = client.post();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("x-foo")).isEqualTo("bar");
    }
}
