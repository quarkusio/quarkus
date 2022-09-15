package io.quarkus.it.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class ClientWithCustomObjectMapperTest {

    MyClient clientAllowsUnknown;
    MyClient clientDisallowsUnknown;
    WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        wireMockServer = new WireMockServer(options().port(20001));
        wireMockServer.start();

        clientAllowsUnknown = RestClientBuilder.newBuilder()
                .baseUrl(new URL(wireMockServer.baseUrl()))
                .register(ClientObjectMapperUnknown.class)
                .build(MyClient.class);

        clientDisallowsUnknown = RestClientBuilder.newBuilder()
                .baseUrl(new URL(wireMockServer.baseUrl()))
                .register(ClientObjectMapperNoUnknown.class)
                .build(MyClient.class);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testCustomObjectMappersShouldBeUsed() {
        var json = "{ \"value\": \"someValue\", \"secondValue\": \"toBeIgnored\" }";
        wireMockServer.stubFor(
                WireMock.get(WireMock.urlMatching("/get"))
                        .willReturn(okJson(json)));

        // FAIL_ON_UNKNOWN_PROPERTIES disabled
        assertThat(clientAllowsUnknown.get().await().indefinitely())
                .isEqualTo(new Request("someValue"));

        // FAIL_ON_UNKNOWN_PROPERTIES enabled
        assertThatThrownBy(() -> clientDisallowsUnknown.get().await().indefinitely())
                .isInstanceOf(ClientWebApplicationException.class);
    }

    @Path("/get")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public interface MyClient {
        @GET
        Uni<Request> get();
    }

    public static class Request {
        private String value;

        public Request() {

        }

        public Request(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Request request = (Request) o;
            return Objects.equals(value, request.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class ClientObjectMapperUnknown implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            return new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }

    public static class ClientObjectMapperNoUnknown implements ContextResolver<ObjectMapper> {
        @Override
        public ObjectMapper getContext(Class<?> type) {
            return new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }
}
