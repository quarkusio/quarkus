package io.quarkus.resteasy.reactive.server.test.http2;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.reactive.common.processor.TargetJavaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.spi.TargetJavaVersionBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * Reproducer for <a href="https://github.com/quarkusio/quarkus/issues/57911">#57911</a>.
 * <p>
 * A {@code @RunOnVirtualThread} endpoint streaming a large response over h2c is
 * repeatedly hit while the client disconnects mid-stream. Before the fix, this
 * deadlocked the event loop (AB-BA between {@code VertxOutputStream}'s lock and
 * the HTTP/2 connection monitor). After the fix, the event loop stays responsive.
 */
@DisabledOnOs(OS.WINDOWS)
public class Http2VirtualThreadDeadlockTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .addBuildChainCustomizer(builder -> builder.addBuildStep(ctx -> ctx.produce(
                    new TargetJavaVersionBuildItem(() -> TargetJavaVersion.Status.TRUE)))
                    .produces(TargetJavaVersionBuildItem.class).build())
            .withApplicationRoot(jar -> jar.addClasses(BigResource.class))
            .overrideRuntimeConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    @Inject
    Vertx vertx;

    @TestHTTPResource("/big")
    URL url;

    @Path("/big")
    public static class BigResource {

        @GET
        @RunOnVirtualThread
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
    void abruptClientDisconnectDoesNotDeadlockEventLoop() {
        for (int i = 0; i < 200; i++) {
            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setHttp2ClearTextUpgrade(false));
            try {
                client.get(url.getPort(), "localhost", "/big")
                        .send()
                        .await().atMost(Duration.ofMillis(50));
            } catch (Exception ignored) {
            } finally {
                client.close();
            }
        }

        // If the event loop deadlocked, this request will time out
        WebClient healthClient = WebClient.create(vertx, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1));
        try {
            HttpResponse<Buffer> response = healthClient.get(url.getPort(), "localhost", "/big")
                    .send()
                    .await().atMost(Duration.ofSeconds(10));
            Assertions.assertEquals(200, response.statusCode());
        } finally {
            healthClient.close();
        }
    }
}
