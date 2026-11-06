package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "http3-test", password = "secret", formats = {
        Format.JKS }))
class Http3NonBlockingTest {

    @TestHTTPResource(value = "/uni", tls = true)
    URL tlsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/http3-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret");

    @Inject
    Vertx vertx;

    record ResponseStruct(String version, String body, int status) {
    }

    private HttpClientAgent createHttp3Client() {
        HttpClientConfig clientConfig = new HttpClientConfig();
        clientConfig.setVersions(HttpVersion.HTTP_3);
        clientConfig.setHttp3Config(new Http3ClientConfig());

        ClientSSLOptions sslOptions = new ClientSSLOptions()
                .setTrustOptions(new JksOptions().setPath("target/certs/http3-test-truststore.jks").setPassword("secret"));

        return vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();
    }

    @Test
    void testHttp3WithUni() throws Exception {
        HttpClientAgent client = createHttp3Client();

        try {
            int port = tlsUrl.getPort();

            var response = client.request(HttpMethod.GET, port, "localhost", "/uni")
                    .compose(HttpClientRequest::send)
                    .compose(resp -> resp.body()
                            .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                    .await(10, TimeUnit.SECONDS);

            assertThat(response.version).isEqualTo("HTTP_3");
            assertThat(response.status).isEqualTo(200);
            assertThat(response.body).isEqualTo("uni-response");
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testHttp3WithDelayedUni() throws Exception {
        HttpClientAgent client = createHttp3Client();

        try {
            int port = tlsUrl.getPort();

            var response = client.request(HttpMethod.GET, port, "localhost", "/delayed")
                    .compose(HttpClientRequest::send)
                    .compose(resp -> resp.body()
                            .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                    .await(10, TimeUnit.SECONDS);

            assertThat(response.version).isEqualTo("HTTP_3");
            assertThat(response.status).isEqualTo(200);
            assertThat(response.body).isEqualTo("delayed-response");
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testHttp3WithCompletionStage() throws Exception {
        HttpClientAgent client = createHttp3Client();

        try {
            int port = tlsUrl.getPort();

            var response = client.request(HttpMethod.GET, port, "localhost", "/completion-stage")
                    .compose(HttpClientRequest::send)
                    .compose(resp -> resp.body()
                            .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                    .await(10, TimeUnit.SECONDS);

            assertThat(response.version).isEqualTo("HTTP_3");
            assertThat(response.status).isEqualTo(200);
            assertThat(response.body).isEqualTo("completion-stage-response");
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testHttp3WithMulti() throws Exception {
        HttpClientAgent client = createHttp3Client();

        try {
            int port = tlsUrl.getPort();

            var response = client.request(HttpMethod.GET, port, "localhost", "/multi")
                    .compose(HttpClientRequest::send)
                    .compose(resp -> resp.body()
                            .map(body -> new ResponseStruct(resp.version().name(), body.toString(), resp.statusCode())))
                    .await(10, TimeUnit.SECONDS);

            assertThat(response.version).isEqualTo("HTTP_3");
            assertThat(response.status).isEqualTo(200);
            assertThat(response.body).contains("item1", "item2", "item3");
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testHttp3WithFailedUni() throws Exception {
        HttpClientAgent client = createHttp3Client();

        try {
            int port = tlsUrl.getPort();

            var response = client.request(HttpMethod.GET, port, "localhost", "/error")
                    .compose(HttpClientRequest::send)
                    .await(10, TimeUnit.SECONDS);

            assertThat(response.version().name()).isEqualTo("HTTP_3");
            assertThat(response.statusCode()).isEqualTo(500);
        } finally {
            client.close().await(5, TimeUnit.SECONDS);
        }
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/uni").handler(rc -> {
                Uni.createFrom().item("uni-response")
                        .subscribe().with(
                                result -> rc.response().end(result),
                                failure -> rc.fail(failure));
            });

            router.get("/delayed").handler(rc -> {
                Uni.createFrom().item("delayed-response")
                        .onItem().delayIt().by(Duration.ofMillis(100))
                        .subscribe().with(
                                result -> rc.response().end(result),
                                failure -> rc.fail(failure));
            });

            router.get("/completion-stage").handler(rc -> {
                CompletionStage<String> future = CompletableFuture.supplyAsync(() -> "completion-stage-response");
                future.whenComplete((result, error) -> {
                    if (error != null) {
                        rc.fail(error);
                    } else {
                        rc.response().end(result);
                    }
                });
            });

            router.get("/multi").handler(rc -> {
                StringBuilder result = new StringBuilder();
                Multi.createFrom().items("item1", "item2", "item3")
                        .subscribe().with(
                                item -> result.append(item).append(" "),
                                failure -> rc.fail(failure),
                                () -> rc.response().end(result.toString().trim()));
            });

            router.get("/error").handler(rc -> {
                Uni.createFrom().<String> failure(new RuntimeException("test-error"))
                        .subscribe().with(
                                result -> rc.response().end(result),
                                failure -> rc.fail(failure));
            });
        }
    }
}
