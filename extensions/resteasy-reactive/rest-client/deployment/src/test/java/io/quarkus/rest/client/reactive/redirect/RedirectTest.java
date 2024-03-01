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
                    .addClasses(RedirectingResourceClient302.class, RedirectingResourceClient307.class,
                            RedirectingResourceWithRegisterProviderRedirectHandlerClient.class,
                            RedirectingResourceWithRedirectHandlerAnnotationClient.class,
                            RedirectingResourceWithSeveralRedirectHandlerAnnotationsClient.class,
                            EnablePostRedirectHandler.class,
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

        RedirectingResourceClient307 client307 = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceClient307.class);
        assertThat(client307.post(1).getStatus()).isEqualTo(307);
    }

    @Test
    void shouldRedirectWhenUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient302 client302 = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                // use custom redirect to enable redirection
                .register(EnablePostRedirectHandler.class)
                .build(RedirectingResourceClient302.class);
        assertThat(client302.post().getStatus()).isEqualTo(200);

        RedirectingResourceClient307 client307 = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                // use custom redirect to enable redirection
                .register(EnablePostRedirectHandler.class)
                .build(RedirectingResourceClient307.class);
        assertThat(client307.post(1).getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRedirectWhenRegisterProviderUsingCustomRedirectHandlerOnPostMethods() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceWithRegisterProviderRedirectHandlerClient.class);
        assertThat(client.post().getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRedirectWhenAnnotatedClientRedirectHandlerOnPostMethods() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceWithRedirectHandlerAnnotationClient.class);
        assertThat(client.post().getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotRedirectWhenARedirectHandlerWithMorePriorityIsUsed() {
        RedirectingResourceClient302 client = RestClientBuilder.newBuilder()
                .baseUri(uri)
                // this property should be ignored in POST
                .followRedirects(true)
                .build(RedirectingResourceWithSeveralRedirectHandlerAnnotationsClient.class);
        assertThat(client.post().getStatus()).isEqualTo(302);
    }
}
