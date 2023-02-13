package io.quarkus.rest.client.reactive;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InvalidHostTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @Test
    void shouldThrowDummyException() {
        Client client = RestClientBuilder.newBuilder().baseUri(URI.create("http://localhost2:1234/"))
                .register(DummyExceptionMapper.class).build(Client.class);

        Assertions.assertThatThrownBy(client::get).isInstanceOf(DummyException.class);
    }

    @Path("/foo")
    public interface Client {
        @GET
        String get();
    }

    public static class DummyException extends RuntimeException {

    }

    public static class DummyExceptionMapper implements ResponseExceptionMapper<DummyException> {

        @Override
        public boolean handles(int status, MultivaluedMap<String, Object> headers) {
            return status == 0 && headers.isEmpty();
        }

        @Override
        public DummyException toThrowable(Response response) {
            return new DummyException();
        }
    }
}
