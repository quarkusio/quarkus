package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ProgrammaticClientExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Dto.class, Client.class, Resource.class, DummyException.class));

    @TestHTTPResource
    URI baseUri;

    Client client;

    @BeforeEach
    void setUp() {
        DummyException.executionCount.set(0);
        client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
    }

    @Test
    void customExceptionMapperEngages() {
        assertThrows(DummyException.class, client::get404);
        assertThat(DummyException.executionCount.get()).isEqualTo(1);
    }

    @Test
    void customExceptionMapperDoesNotEngage() {
        assertThrows(RuntimeException.class, client::get400);
        assertThat(DummyException.executionCount.get()).isEqualTo(0);
    }

    @Path("/error")
    public interface Client {
        @Path("/404")
        @GET
        Dto get404();

        @Path("/400")
        @GET
        Dto get400();

        @ClientExceptionMapper
        static DummyException map(Response response) {
            if (response.getStatus() == 404) {
                return new DummyException();
            }
            return null;
        }
    }

}
