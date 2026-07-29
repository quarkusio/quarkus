package io.quarkus.rest.client.reactive.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class NotModifiedWithRequestCustomizerTest {

    /**
     * Bounds the wait, so a response that never arrives fails the test instead of blocking it forever.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Client.class, RedirectingResource.class));

    @TestHTTPResource
    URI uri;

    private Client clientWithCustomizer(AtomicInteger customizerInvocations) {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(uri)
                .followRedirects(true)
                .httpClientRequestCustomizer(request -> customizerInvocations.incrementAndGet())
                .build(Client.class);
    }

    /**
     * A 304 has no Location header, so there is no redirect to follow even though the status is within the range
     * that Vert.x hands to the redirect handler.
     */
    @Test
    void shouldReturnNotModifiedWhenThereIsNoRedirectToFollow() {
        AtomicInteger customizerInvocations = new AtomicInteger();

        Response response = clientWithCustomizer(customizerInvocations)
                .notModified(RedirectingResource.ETAG)
                .await().atMost(TIMEOUT);

        assertThat(response.getStatus()).isEqualTo(304);
        assertThat(customizerInvocations).hasValue(1);
    }

    @Test
    void shouldApplyCustomizerToRedirectRequest() {
        AtomicInteger customizerInvocations = new AtomicInteger();

        Response response = clientWithCustomizer(customizerInvocations)
                .redirected(1)
                .await().atMost(TIMEOUT);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(customizerInvocations).hasValue(2);
    }

    @Path("/redirect")
    public interface Client {

        @GET
        @Path("304")
        Uni<Response> notModified(@HeaderParam(HttpHeaders.IF_NONE_MATCH) String ifNoneMatch);

        @GET
        @Path("302")
        Uni<Response> redirected(@QueryParam("redirects") Integer redirects);
    }
}
