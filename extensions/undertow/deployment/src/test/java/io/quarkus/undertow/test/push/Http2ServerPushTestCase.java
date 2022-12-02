package io.quarkus.undertow.test.push;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;

public class Http2ServerPushTestCase {

    @TestHTTPResource(value = "/push", ssl = true)
    URL sslUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ServerPushServlet.class, MessageServlet.class)
                    .addAsResource(new File("src/test/resources/ssl-jks.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/server-keystore.jks"), "server-keystore.jks"));

    @Test
    public void testServerPush() throws Exception {
        Assumptions.assumeTrue(JdkSSLEngineOptions.isAlpnAvailable()); //don't run on JDK8
        Vertx vertx = Vertx.vertx();
        try {
            HttpClientOptions options = new HttpClientOptions().setSsl(true).setUseAlpn(true)
                    .setProtocolVersion(HttpVersion.HTTP_2).setVerifyHost(false).setTrustAll(true);

            final CompletableFuture<String> pushedPath = new CompletableFuture<>();
            final CompletableFuture<String> pushedBody = new CompletableFuture<>();

            // Vert.x 4 Migration: review for correctness
            //
            //            request.pushHandler(new Handler<HttpClientRequest>() {
            //                @Override
            //                public void handle(HttpClientRequest event) {
            //                    pushedPath.complete(event.path());
            //                    event.handler(new Handler<HttpClientResponse>() {
            //                        @Override
            //                        public void handle(HttpClientResponse event) {
            //                            event.bodyHandler(new Handler<Buffer>() {
            //                                @Override
            //                                public void handle(Buffer event) {
            //                                    pushedBody.complete(new String(event.getBytes(), StandardCharsets.UTF_8));
            //                                }
            //                            });
            //                            event.exceptionHandler(new Handler<Throwable>() {
            //                                @Override
            //                                public void handle(Throwable event) {
            //                                    pushedBody.completeExceptionally(event);
            //                                    pushedPath.completeExceptionally(event);
            //                                }
            //                            });
            //                        }
            //                    });
            //                }
            //            });
            //            request.handler(new Handler<HttpClientResponse>() {
            //                @Override
            //                public void handle(HttpClientResponse event) {
            //                    event.endHandler(new Handler<Void>() {
            //                        @Override
            //                        public void handle(Void event) {
            //
            //                        }
            //                    });
            //                }
            //            });
            //            request.end();

            HttpClient client = vertx.createHttpClient(options);
            client.request(HttpMethod.GET, sslUrl.getPort(), sslUrl.getHost(), sslUrl.getPath())
                    .toCompletionStage()
                    .thenAccept(new Consumer<HttpClientRequest>() {
                        @Override
                        public void accept(HttpClientRequest req) {
                            req.response().onComplete(new Handler<AsyncResult<HttpClientResponse>>() {
                                @Override
                                public void handle(AsyncResult<HttpClientResponse> r) {
                                    // Ignored
                                }
                            });

                            req.pushHandler(new Handler<HttpClientRequest>() {
                                @Override
                                public void handle(HttpClientRequest pushedRequest) {
                                    pushedPath.complete(pushedRequest.path());
                                    pushedRequest.response().toCompletionStage()
                                            .thenCompose(new Function<HttpClientResponse, CompletionStage<Buffer>>() {
                                                @Override
                                                public CompletionStage<Buffer> apply(
                                                        HttpClientResponse resp) {
                                                    return resp.body().toCompletionStage();
                                                }
                                            })
                                            .thenAccept(new Consumer<Buffer>() {
                                                @Override
                                                public void accept(Buffer buffer) {
                                                    pushedBody.complete(
                                                            new String(buffer.getBytes(), StandardCharsets.UTF_8));
                                                }
                                            });
                                }
                            });

                            req.end();
                        }
                    });

            Assertions.assertEquals("/pushed", pushedPath.get(10, TimeUnit.SECONDS));
            Assertions.assertEquals("pushed-body", pushedBody.get(10, TimeUnit.SECONDS));

        } finally {
            vertx.close();
        }

    }

}
