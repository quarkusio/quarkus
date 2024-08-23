package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.net.URI;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;

public class SendMultiBufferTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI uri;

    @Test
    public void test() throws FileNotFoundException {
        Multi<io.vertx.mutiny.core.buffer.Buffer> multi = Multi.createFrom().emitter(e -> {
            for (int i = 0; i < 1000; i++) {
                e.emit(Buffer.buffer(String.format("%03d", i)));
            }
            e.complete();
        });
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        long result = client.count(multi);

        assertEquals(3000, result);
    }

    @Path("test")
    public interface Client {

        @POST
        @Path("count")
        long count(Multi<io.vertx.mutiny.core.buffer.Buffer> multi);
    }

    @Path("test")
    public static class Resource {

        @POST
        @Path("count")
        public long count(String input) {
            return input.length();
        }
    }
}
