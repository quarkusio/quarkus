package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SendInputStreamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    private final File FILE = new File("./src/test/resources/larger-than-chunk-size.txt");

    @TestHTTPResource
    URI uri;

    @Test
    public void test() throws FileNotFoundException {
        Client client = RestClientBuilder.newBuilder().baseUri(uri).build(Client.class);

        InputStream is = new FileInputStream(FILE);
        long result = client.count(is);

        assertEquals(FILE.length(), result);
    }

    @Path("test")
    public interface Client {

        @POST
        @Path("count")
        long count(InputStream is);
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
