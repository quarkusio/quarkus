package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

public class ReactiveClientHeadersFromProviderTest {
    private static final String HEADER_NAME = "my-header";
    private static final String HEADER_VALUE = "oifajrofijaeoir5gjaoasfaxcvcz";

    @TestHTTPResource
    URI baseUri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(ReactiveClientHeadersFromProviderTest.Client.class)
                    .addAsResource(
                            new StringAsset("my.property-value=" + HEADER_VALUE),
                            "application.properties"));

    @Test
    void shouldSetHeaderFromProperties() {
        ReactiveClientHeadersFromProviderTest.Client client = RestClientBuilder.newBuilder().baseUri(baseUri)
                .build(ReactiveClientHeadersFromProviderTest.Client.class);

        assertThat(client.getWithHeader()).isEqualTo(HEADER_VALUE);
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaderValue(@HeaderParam(HEADER_NAME) String header) {
            return header;
        }
    }

    @ApplicationScoped
    public static class Service {
        @Blocking
        public String getValue() {
            return HEADER_VALUE;
        }
    }

    @RegisterClientHeaders(CustomReactiveClientHeadersFactory.class)
    public interface Client {
        @GET
        String getWithHeader();
    }

    public static class CustomReactiveClientHeadersFactory extends ReactiveClientHeadersFactory {

        @Inject
        Service service;

        @Override
        public Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders) {
            return Uni.createFrom().item(() -> {
                MultivaluedHashMap<String, String> newHeaders = new MultivaluedHashMap<>();
                newHeaders.add(HEADER_NAME, service.getValue());
                return newHeaders;
            });
        }
    }
}
