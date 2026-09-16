package io.quarkus.resteasy.test.servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A client going away while a chunked response is being written must not be reported as an error: the write to the
 * closed connection fails, nothing can be sent to the client any more, and that is expected.
 */
public class ClientAbortChunkedResponseTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(ClientAbortResource.class))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-undertow", Version.getVersion())))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && record.getLoggerName().startsWith("io.undertow"))
            .assertLogRecords(records -> assertTrue(records.isEmpty(), () -> "Unexpected log records: " + records.stream()
                    .map(r -> r.getLevel() + " " + r.getLoggerName() + ": " + r.getMessage()).toList()));

    @Path("/client-abort")
    public static class ClientAbortResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() throws InterruptedException {
            // gives the client time to go away; the response is larger than the buffer so it is chunked
            Thread.sleep(500);
            return "A".repeat(64 * 1024);
        }
    }

    @Test
    public void clientClosesTheConnectionBeforeTheChunkedResponse() throws Exception {
        try (Socket socket = new Socket("localhost", RestAssured.port)) {
            OutputStream out = socket.getOutputStream();
            out.write(("GET /client-abort HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(100);
        }
        // let the endpoint complete and attempt to write to the closed connection
        Thread.sleep(1500);
        // and the server still works
        RestAssured.get("/client-abort").then().statusCode(200);
    }
}
