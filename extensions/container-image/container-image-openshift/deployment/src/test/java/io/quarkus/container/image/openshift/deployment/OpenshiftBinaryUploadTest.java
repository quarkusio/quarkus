package io.quarkus.container.image.openshift.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.smallrye.config.ConfigInstanceBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An interrupted binary upload must not be retried by the HTTP client: the archive is streamed from a single
 * {@code InputStream} that cannot be replayed, so a retry creates another build fed with an already consumed stream.
 * See https://github.com/quarkusio/quarkus/issues/57420.
 */
class OpenshiftBinaryUploadTest {

    private final AtomicInteger uploads = new AtomicInteger();
    private Vertx vertx;
    private HttpServer server;

    @BeforeEach
    void startFakeApiServer() throws Exception {
        vertx = Vertx.vertx();
        // The upload uses "Expect: 100-continue": without this the body is never sent and the request times out instead
        server = vertx.createHttpServer(new HttpServerOptions().setHandle100ContinueAutomatically(true))
                .requestHandler(request -> {
                    if (request.method() == HttpMethod.POST && request.path().endsWith("/instantiatebinary")) {
                        uploads.incrementAndGet();
                        // drop the connection as soon as the archive starts flowing
                        request.handler(chunk -> request.connection().close());
                    } else {
                        request.response().putHeader("Content-Type", "application/json")
                                .end(new JsonObject().put("kind", "EventList").put("apiVersion", "v1")
                                        .put("items", new JsonArray()).encode());
                    }
                })
                .listen(0)
                .await(10, TimeUnit.SECONDS);
    }

    @AfterEach
    void stopFakeApiServer() throws Exception {
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    @Test
    void interruptedUploadIsNotRetried(@TempDir Path tempDir) throws Exception {
        File archive = Files.write(tempDir.resolve("archive.tar"), new byte[8 * 1024 * 1024]).toFile();
        KubernetesClientBuilder clientBuilder = new KubernetesClientBuilder().withConfig(new ConfigBuilder()
                .withMasterUrl("http://localhost:" + server.actualPort())
                .withNamespace("test")
                .withAutoConfigure(false) // do not probe ~/.kube/config or the cluster environment
                .withHttp2Disable(true) // the fake server only speaks HTTP/1.1
                .withRequestRetryBackoffLimit(2) // allow retries so we can verify our code disables them for uploads
                .withRequestRetryBackoffInterval(10) // keep the backoff short so the test fails fast if retries leak
                .build());
        BuildConfig buildConfig = new BuildConfigBuilder().withNewMetadata().withName("app").endMetadata().build();

        ContainerImageOpenshiftConfig openshiftConfig = ConfigInstanceBuilder
                .forInterface(ContainerImageOpenshiftConfig.class)
                .with(ContainerImageOpenshiftConfig::buildTimeout, Duration.ofSeconds(30))
                .with(ContainerImageOpenshiftConfig::failOnBuildArchiveUploadFailure, true)
                .build();

        KubernetesClientException e = assertThrows(KubernetesClientException.class,
                () -> OpenshiftProcessor.startOpenshiftBuild(buildConfig, archive, openshiftConfig, clientBuilder));
        // The dropped connection, as opposed to another failure such as a request timeout
        assertInstanceOf(IOException.class, e.getCause());

        // Each retry would be a new POST creating another build, fed with an already consumed stream
        assertEquals(1, uploads.get());
    }
}
