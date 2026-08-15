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
 * Verifies that plain HTTP requests are redirected to HTTPS when HTTP/3 auto-TLS is active.
 */
class Http3AutoTlsRedirectTest {

    @TestHTTPResource(value = "/hello")
    URL httpUrl;

    @TestHTTPResource(value = "/hello", tls = true)
    URL httpsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyBean.class));

    @Inject
    Vertx vertx;

    @Test
    void testHttpRedirectsToHttps() throws Exception {
        HttpClientConfig clientConfig = new HttpClientConfig();
        HttpClientAgent client = vertx.httpClientBuilder().with(clientConfig).build();

        var response = client.request(HttpMethod.GET, httpUrl.getPort(), "localhost", "/hello")
                .compose(HttpClientRequest::send)
                .await(10, TimeUnit.SECONDS);

        assertThat(response.statusCode()).isEqualTo(301);
        String location = response.getHeader("Location");
        assertThat(location).startsWith("https://");
        assertThat(location).contains("/hello");
        assertThat(location).contains(":" + httpsUrl.getPort());

        client.close().await(5, TimeUnit.SECONDS);
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/hello").handler(rc -> rc.response().end("redirect-test"));
        }
    }
}
