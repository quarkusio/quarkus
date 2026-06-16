package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

/**
 * Verifies that explicit {@code quarkus.http.insecure-requests=enabled} overrides the
 * auto-TLS redirect default, allowing plain HTTP to work normally.
 */
class Http3AutoTlsInsecureEnabledTest {

    @TestHTTPResource(value = "/hello")
    URL httpUrl;

    // No TLS config: auto-TLS will fire, but explicit insecure-requests=enabled overrides redirect
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyBean.class))
            .overrideConfigKey("quarkus.http.insecure-requests", "enabled");

    @Inject
    Vertx vertx;

    @Test
    void testHttpNotRedirected() throws Exception {
        HttpClientConfig clientConfig = new HttpClientConfig();
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).build();

        var response = client.request(HttpMethod.GET, httpUrl.getPort(), "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .compose(resp -> resp.body().map(body -> new Result(resp.statusCode(), body.toString())))
                .await(10, TimeUnit.SECONDS);

        assertThat(response.status).isEqualTo(200);
        assertThat(response.body).isEqualTo("insecure-ok");

        client.close().await(5, TimeUnit.SECONDS);
    }

    record Result(int status, String body) {
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("insecure-ok"));
        }
    }
}
