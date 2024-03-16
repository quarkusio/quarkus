package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.http.HttpClientOptions;

public class CustomHttpOptionsViaInjectedRestClientTest {

    private static final String EXPECTED_VALUE = "success";

    @TestHTTPResource
    URI baseUri;

    @RestClient
    Client client;

    @RegisterExtension
    static final QuarkusUnitTest app = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, CustomHttpClientOptionsWithLimit.class)
                    .addAsResource(new StringAsset(
                            "custom-http-options/mp-rest/url=http://localhost:${quarkus.http.test-port:8081}"),
                            "application.properties"));

    @Test
    void shouldUseCustomHttpOptions() {
        assertThatThrownBy(() -> client.get()).hasMessageContaining("HTTP header is larger than 1 bytes.");
    }

    @Test
    void shouldProgrammaticallyCreatedUseTheCustomHttpOptions() {
        Client programmaticClient = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThatThrownBy(() -> programmaticClient.get()).hasMessageContaining("HTTP header is larger than 1 bytes.");
    }

    @Test
    void shouldCustomHttpOptionsFromRegisterHavePriorityOverCDI() {
        Client programmaticClient = RestClientBuilder.newBuilder().baseUri(baseUri)
                .register(CustomHttpClientOptionsWithoutLimit.class) // reset HTTP Header size limit
                .build(Client.class);
        assertThat(programmaticClient.get()).isEqualTo(EXPECTED_VALUE);
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

    @Provider
    public static class CustomHttpClientOptionsWithLimit implements ContextResolver<HttpClientOptions> {

        @Override
        public HttpClientOptions getContext(Class<?> aClass) {
            HttpClientOptions options = new HttpClientOptions();
            options.setMaxHeaderSize(1); // this is just to verify that this HttpClientOptions is indeed used.
            return options;
        }
    }

    public static class CustomHttpClientOptionsWithoutLimit implements ContextResolver<HttpClientOptions> {

        @Override
        public HttpClientOptions getContext(Class<?> aClass) {
            HttpClientOptions options = new HttpClientOptions();
            return options;
        }
    }

    @RegisterRestClient(configKey = "custom-http-options")
    public interface Client {
        @GET
        String get();
    }
}
