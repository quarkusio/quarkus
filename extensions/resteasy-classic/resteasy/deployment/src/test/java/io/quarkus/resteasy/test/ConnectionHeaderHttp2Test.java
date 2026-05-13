package io.quarkus.resteasy.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS }))
class ConnectionHeaderHttp2Test {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Resource.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret");

    @TestHTTPResource(value = "/", tls = true)
    URL tlsUrl;

    @TestHTTPResource(value = "/")
    URL plainUrl;

    @Test
    void headOverHttp2ShouldNotHaveConnectionHeader() throws Exception {
        HttpClient client = VertxCoreRecorder.getVertx().get()
                .createHttpClient(new HttpClientOptions()
                        .setProtocolVersion(HttpVersion.HTTP_2)
                        .setSsl(true)
                        .setUseAlpn(true)
                        .setTrustOptions(new JksOptions()
                                .setPath("target/certs/ssl-test-truststore.jks")
                                .setPassword("secret")));

        assertNoConnectionHeaderOverHttp2(client, tlsUrl);
    }

    @Test
    void headOverH2cShouldNotHaveConnectionHeader() throws Exception {
        HttpClient client = VertxCoreRecorder.getVertx().get()
                .createHttpClient(new HttpClientOptions()
                        .setProtocolVersion(HttpVersion.HTTP_2)
                        .setHttp2ClearTextUpgrade(false));

        assertNoConnectionHeaderOverHttp2(client, plainUrl);
    }

    @Test
    void headOverHttp1ShouldHaveConnectionKeepAlive() {
        RestAssured.when().head("/connection-header/hello")
                .then()
                .statusCode(200)
                .header("Connection", "keep-alive");
    }

    private void assertNoConnectionHeaderOverHttp2(HttpClient client, URL url) throws Exception {
        try {
            CompletableFuture<HttpClientResponse> result = new CompletableFuture<>();
            client.request(HttpMethod.HEAD, url.getPort(), "localhost", "/connection-header/hello")
                    .onSuccess(req -> req.send()
                            .onSuccess(result::complete)
                            .onFailure(result::completeExceptionally))
                    .onFailure(result::completeExceptionally);

            HttpClientResponse response = result.get(10, TimeUnit.SECONDS);
            assertEquals(HttpVersion.HTTP_2, response.version());
            assertNull(response.getHeader("connection"),
                    "Connection header must not be set on HTTP/2 responses");
        } finally {
            client.close();
        }
    }

    @Path("/connection-header")
    public static class Resource {

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }
    }
}
