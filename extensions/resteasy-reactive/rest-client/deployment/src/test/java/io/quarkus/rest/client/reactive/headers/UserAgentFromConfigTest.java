package io.quarkus.rest.client.reactive.headers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class UserAgentFromConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class)
                    .addAsResource(new StringAsset(
                            "quarkus.rest-client.user-agent=base\n" +
                                    "quarkus.rest-client.client1.url=http://localhost:${quarkus.http.test-port:8081}\n" +
                                    "quarkus.rest-client.client2.url=http://localhost:${quarkus.http.test-port:8081}\n" +
                                    "quarkus.rest-client.client2.user-agent=specific"),
                            "application.properties"));

    @TestHTTPResource
    URI baseUri;

    @RestClient
    Client client;

    @RestClient
    Client2 client2;

    @Test
    void testProgrammatic() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.call()).isEqualTo("base");
    }

    @Test
    void testBaseUserAgent() {
        assertThat(client.call()).isEqualTo("base");
    }

    @Test
    void testSpecificUserAgent() {
        assertThat(client2.call()).isEqualTo("specific");
    }

    @Path("/")
    @ApplicationScoped
    public static class Resource {
        @GET
        public String returnHeaders(@HeaderParam("user-agent") String header) {
            return header;
        }
    }

    @RegisterRestClient(configKey = "client1")
    public interface Client {

        @Path("/")
        @GET
        String call();
    }

    @RegisterRestClient(configKey = "client2")
    public interface Client2 {

        @Path("/")
        @GET
        String call();
    }

}
