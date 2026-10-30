package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Uni;

public class ReactiveClientHeadersFactoryNullTest {

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class, NullHeadersFactory.class));

    @Test
    void shouldFailInsteadOfHangingWhenHeadersAreNull() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        CompletableFuture<String> response = CompletableFuture.supplyAsync(client::get);
        assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(() -> response.get(10, TimeUnit.SECONDS))
                .withRootCauseInstanceOf(NullPointerException.class);
    }

    @Path("/")
    public static class Resource {

        @GET
        public String get() {
            return "ok";
        }
    }

    @RegisterClientHeaders(NullHeadersFactory.class)
    public interface Client {

        @GET
        String get();
    }

    public static class NullHeadersFactory extends ReactiveClientHeadersFactory {

        @Override
        public Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders,
                MultivaluedMap<String, String> clientOutgoingHeaders) {
            return Uni.createFrom().nullItem();
        }
    }
}
