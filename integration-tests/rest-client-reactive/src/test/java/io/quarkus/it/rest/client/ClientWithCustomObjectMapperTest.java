package io.quarkus.it.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
public class ClientWithCustomObjectMapperTest {

    MyClient clientAllowsUnknown;
    MyClient clientDisallowsUnknown;
    WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        ClientObjectMapperUnknown.USED.set(false);
        ClientObjectMapperNoUnknown.USED.set(false);
        wireMockServer = new WireMockServer(options().port(20001));
        wireMockServer.start();

        clientAllowsUnknown = QuarkusRestClientBuilder.newBuilder()
                .baseUrl(new URL(wireMockServer.baseUrl()))
                .register(ClientObjectMapperUnknown.class)
                .build(MyClient.class);

        clientDisallowsUnknown = QuarkusRestClientBuilder.newBuilder()
                .baseUrl(new URL(wireMockServer.baseUrl()))
                .register(ClientObjectMapperNoUnknown.class)
                .build(MyClient.class);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testCustomObjectMappersShouldBeUsedInReader() {
        var json = "{ \"value\": \"someValue\", \"secondValue\": \"toBeIgnored\" }";
        wireMockServer.stubFor(
                WireMock.get(WireMock.urlMatching("/client"))
                        .willReturn(okJson(json)));

        // FAIL_ON_UNKNOWN_PROPERTIES disabled
        clientAllowsUnknown.get().subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem().assertItem(new Request("someValue"));

        // FAIL_ON_UNKNOWN_PROPERTIES enabled
        clientDisallowsUnknown.get().subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(UnrecognizedPropertyException.class);
    }

    @Test
    void testCustomObjectMappersShouldBeUsedInWriter() {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlMatching("/client"))
                        .willReturn(ok()));

        clientDisallowsUnknown.post(new Request());
        assertThat(ClientObjectMapperNoUnknown.USED.get()).isTrue();
    }

    @Path("/client")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public interface MyClient {
        @GET
        Uni<Request> get();

        @POST
        void post(Request request);
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
        static final AtomicBoolean USED = new AtomicBoolean(false);

        @Override
        public ObjectMapper getContext(Class<?> type) {
            USED.set(true);
            return new ObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }

    public static class ClientObjectMapperNoUnknown implements ContextResolver<ObjectMapper> {

        static final AtomicBoolean USED = new AtomicBoolean(false);

        @Override
        public ObjectMapper getContext(Class<?> type) {
            USED.set(true);
            return new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
    }
}
