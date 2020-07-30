package io.quarkus.it.main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.faulttolerance.FaultToleranceTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FaultToleranceTestCase {

    @TestHTTPEndpoint(FaultToleranceTestResource.class)
    @TestHTTPResource
    URL uri;

    @Test
    public void testRetry() throws Exception {
        URLConnection connection = uri.openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = connection.getInputStream()) {
            byte[] buf = new byte[100];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        }
        Assertions.assertEquals("2:Lucie", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}
