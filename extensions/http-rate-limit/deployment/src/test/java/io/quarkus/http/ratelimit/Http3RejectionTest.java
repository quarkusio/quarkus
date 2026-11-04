package io.quarkus.http.ratelimit;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ClientSSLOptions;

/**
 * The limiter over HTTP/3, with the {@code quarkus-http3} extension and its generated development certificate: a
 * refused request is answered like over HTTP/1.1, and one whose body exceeds {@code max-body-size} ends its own stream,
 * not the QUIC connection that carries the other requests.
 */
public class Http3RejectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1h")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class,
                    MultiplexedRejections.class, MultiplexedRejections.SlowRoute.class))
            .overrideConfigKey("quarkus.http.limits.max-body-size", "1K")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-http3-deployment", Version.getVersion())));

    @Inject
    Vertx vertx;

    @TestHTTPResource(tls = true)
    URL url;

    HttpClientAgent client;

    @BeforeEach
    void createClient() {
        HttpClientConfig clientConfig = new HttpClientConfig().setVersions(HttpVersion.HTTP_3)
                .setHttp3Config(new Http3ClientConfig());
        // the development certificate is not trusted
        ClientSSLOptions sslOptions = new ClientSSLOptions().setTrustAll(true).setHostnameVerificationAlgorithm("");
        client = vertx.httpClientBuilder().with(clientConfig).with(sslOptions).build();
    }

    @AfterEach
    void closeClient() throws TimeoutException {
        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void rejectionIsAnswered() throws TimeoutException {
        MultiplexedRejections.rejectionIsAnswered(client, url.getPort(), HttpVersion.HTTP_3);
    }

    @Test
    void declaredBodyOverTheMaxBodySizeEndsOnlyItsOwnStream() throws TimeoutException {
        MultiplexedRejections.declaredBodyEndsOnlyItsOwnStream(client, url.getPort());
    }

    @Test
    void streamedBodyOverTheMaxBodySizeEndsOnlyItsOwnStream() throws TimeoutException {
        MultiplexedRejections.streamedBodyEndsOnlyItsOwnStream(client, url.getPort());
    }
}
