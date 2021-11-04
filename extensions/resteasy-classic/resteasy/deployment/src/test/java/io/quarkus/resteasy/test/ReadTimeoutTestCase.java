package io.quarkus.resteasy.test;

import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class ReadTimeoutTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.read-timeout=0.5S"), "application.properties")
                    .addClasses(PostEndpoint.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testReadTimesOut() throws Exception {
        PostEndpoint.invoked = false;

        //make sure incomplete writes do not block threads
        //and that incoplete data is not delivered to the endpoint
        Socket socket = new Socket(url.getHost(), url.getPort());
        socket.getOutputStream().write(
                "POST /post HTTP/1.1\r\nHost: localhost\r\nContent-length:9\r\n\r\n12345".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(600);
        socket.getOutputStream().write(
                "6789".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(600);
        //make sure the read timed out and the endpoint was not invoked
        Assertions.assertFalse(PostEndpoint.invoked);
        socket.close();
    }

    @Test
    public void testReadSuccess() throws Exception {
        PostEndpoint.invoked = false;

        //make sure incomplete writes do not block threads
        //and that incoplete data is not delivered to the endpoint
        Socket socket = new Socket(url.getHost(), url.getPort());
        socket.getOutputStream().write(
                "POST /post HTTP/1.1\r\nHost: localhost\r\nContent-length:9\r\n\r\n12345".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Thread.sleep(1);
        socket.getOutputStream().write(
                "6789".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
        Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        //make sure the read timed out and the endpoint was invoked
                        Assertions.assertTrue(PostEndpoint.invoked);
                    }
                });
        socket.close();
    }

}
