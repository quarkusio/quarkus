package io.quarkus.undertow.test.timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ExpectLogMessage;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ReadTimeoutTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.http.read-timeout", "0.5S")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TimeoutTestServlet.class));

    private String host;
    private SocketChannel client;

    public static volatile String READ_DATA;

    @BeforeEach
    public void init() throws IOException {
        int port = RestAssured.port;
        host = URI.create(RestAssured.baseURI).getHost();
        InetSocketAddress hostAddress = new InetSocketAddress(host, port);
        client = SocketChannel.open(hostAddress);
        TimeoutTestServlet.reset();
    }

    @AfterEach
    public void cleanUp() throws IOException {
        client.close();
    }

    @Test
    @ExpectLogMessage("Exception handling request")
    public void shouldNotProcessRequestWrittenTooSlowly() throws IOException, InterruptedException {
        requestWithDelay(2000L);

        ByteBuffer buffer = ByteBuffer.allocate(100000);
        try {
            client.read(buffer);
        } catch (IOException ignore) {

        }

        assertFalse(TimeoutTestServlet.read, "Did not expect data to be read, content was: " + READ_DATA);
        assertNotNull(TimeoutTestServlet.error);
    }

    @Test
    public void shouldProcessSlowlyProcessedRequest() throws IOException, InterruptedException {
        requestWithDelay(100L, "Processing-Time: 1000");

        ByteBuffer buffer = ByteBuffer.allocate(100000);
        client.read(buffer);
        MatcherAssert.assertThat(new String(buffer.array(), StandardCharsets.UTF_8),
                Matchers.containsString(TimeoutTestServlet.TIMEOUT_SERVLET));
        assertTrue(TimeoutTestServlet.read);
    }

    private void requestWithDelay(long sleepTime, String... headers)
            throws IOException, InterruptedException {
        String content = "message content";
        writeToChannel("POST /timeout HTTP/1.1\r\n");
        writeToChannel("Content-Length: " + ("The \r\n" + content).getBytes("UTF-8").length);
        for (String header : headers) {
            writeToChannel("\r\n" + header);
        }
        writeToChannel("\r\nHost: " + host);
        writeToChannel("\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
        writeToChannel("The \r\n");
        Thread.sleep(sleepTime);
        writeToChannel(content);
    }

    private void writeToChannel(String s) {
        try {
            byte[] message = s.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(message);
            client.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to channel", e);
        }
    }
}
