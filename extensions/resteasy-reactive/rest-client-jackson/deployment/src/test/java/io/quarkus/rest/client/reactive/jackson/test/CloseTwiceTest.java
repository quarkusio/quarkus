package io.quarkus.rest.client.reactive.jackson.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.net.URI;
import java.util.logging.LogRecord;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.client.impl.RestClientClosingTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * {@link Closeable#close()} has no effect when the client is already closed, so the closing tasks must not fail on
 * the second call.
 */
public class CloseTwiceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Client.class, Resource.class))
            .setLogRecordPredicate(record -> RestClientClosingTask.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage).isEmpty());

    @TestHTTPResource
    URI uri;

    @Test
    void closeTwice() throws Exception {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(uri).build(Client.class);
        assertThat(client.hello()).isEqualTo("hello");

        client.close();
        client.close();
    }

    @Path("/hello")
    public interface Client extends Closeable {

        @GET
        String hello();
    }

    @Path("/hello")
    public static class Resource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
