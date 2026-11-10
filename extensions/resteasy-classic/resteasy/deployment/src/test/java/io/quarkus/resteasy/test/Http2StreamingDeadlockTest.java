package io.quarkus.resteasy.test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.http.*;

@DisabledOnOs(OS.WINDOWS)
public class Http2StreamingDeadlockTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(BigResource.class))
            .overrideRuntimeConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    @TestHTTPResource("/big")
    URL url;

    @Path("/big")
    public static class BigResource {

        @GET
        public Response big() {
            StreamingOutput s = new StreamingOutput() {
                @Override
                public void write(OutputStream out) throws IOException {
                    byte[] buf = new byte[8192];
                    for (int i = 0; i < 1024; i++) {
                        out.write(buf);
                    }
                }
            };
            return Response.ok(s).build();
        }
    }

    @Test
    void abruptClientDisconnectDoesNotDeadlockEventLoop() throws Exception {
        for (int i = 0; i < 200; i++) {
            HttpClient client = VertxCoreRecorder.getVertx().get()
                    .createHttpClient(new HttpClientOptions()
                            .setProtocolVersion(HttpVersion.HTTP_2)
                            .setHttp2ClearTextUpgrade(false));
            try {
                client.request(HttpMethod.GET, url.getPort(), "localhost", "/big")
                        .compose(HttpClientRequest::send)
                        .await(50, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            } finally {
                client.close();
            }
        }

        HttpClient healthClient = VertxCoreRecorder.getVertx().get()
                .createHttpClient(new HttpClientOptions()
                        .setProtocolVersion(HttpVersion.HTTP_1_1));
        try {
            HttpClientResponse response = healthClient
                    .request(HttpMethod.GET, url.getPort(), "localhost", "/big")
                    .compose(HttpClientRequest::send)
                    .await(10, TimeUnit.SECONDS);
            Assertions.assertEquals(200, response.statusCode());
        } finally {
            healthClient.close();
        }
    }
}
