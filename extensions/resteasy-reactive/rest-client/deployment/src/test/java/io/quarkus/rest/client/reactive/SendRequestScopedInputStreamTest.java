package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class SendRequestScopedInputStreamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withConfigurationResource("send-request-scoped-input-stream.properties");

    @Test
    public void test() {
        when()
                .get("test/in")
                .then()
                .statusCode(200)
                .body(equalTo("" + CustomInputStream.MAX_ITERATIONS));

        // make sure the request scope of the stream is coming into play
        when()
                .get("test/in")
                .then()
                .statusCode(200)
                .body(equalTo("" + CustomInputStream.MAX_ITERATIONS));

        when()
                .get("test/uniIn")
                .then()
                .statusCode(200)
                .body(equalTo("" + CustomInputStream.MAX_ITERATIONS));
    }

    @RegisterRestClient(configKey = "test")
    @Path("test")
    public interface Client {

        @Path("out")
        @POST
        long count(InputStream is);

        @Path("out")
        @POST
        Uni<Long> uniCount(InputStream is);
    }

    @Path("test")
    public static class Resource {

        private final CustomInputStream is;
        private final Client client;

        public Resource(CustomInputStream is, @RestClient Client client) {
            this.is = is;
            this.client = client;
        }

        @Path("in")
        @GET
        public long in() {
            return client.count(is);
        }

        @Path("uniIn")
        @GET
        public Uni<Long> uniIn() {
            return client.uniCount(is);
        }

        @Path("out")
        @POST
        public long out(String input) {
            return input.length();
        }
    }

    @RequestScoped
    public static class CustomInputStream extends InputStream {

        private static final int MAX_ITERATIONS = 100;
        private final AtomicLong count = new AtomicLong();

        @Override
        public int read() throws IOException {
            if (!BlockingOperationControl.isBlockingAllowed()) {
                throw new BlockingOperationNotAllowedException("The read method of the stream was called an event loop thread");
            }
            if (count.incrementAndGet() <= MAX_ITERATIONS) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {

                }
                return 'A'; // we don't really care about what we return
            }
            return -1; // signal the end of the stream
        }
    }
}
