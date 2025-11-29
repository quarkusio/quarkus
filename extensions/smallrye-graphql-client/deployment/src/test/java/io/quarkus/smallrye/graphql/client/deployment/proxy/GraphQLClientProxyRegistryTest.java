package io.quarkus.smallrye.graphql.client.deployment.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;

public class GraphQLClientProxyRegistryTest {

    static String targetUrl = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DummyApi.class)
                    .addAsResource(
                            new StringAsset("""
                                    quarkus.smallrye-graphql-client.dummy.url=%s
                                    quarkus.smallrye-graphql-client.dummy.proxy-configuration-name=MYPROXY
                                    quarkus.proxy.MYPROXY.host=localhost
                                    quarkus.proxy.MYPROXY.port=3456
                                    """.formatted(targetUrl)),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    private static Vertx vertx;
    private static HttpServer proxyServer;
    private static HttpClient proxyClient;
    private static final AtomicBoolean wasProxyCalled = new AtomicBoolean(false);

    /**
     * Starts a simple HTTP proxy server that forwards requests to the final destination.
     */
    @BeforeAll
    public static void startProxyServer() throws ExecutionException, InterruptedException {
        vertx = Vertx.vertx();
        proxyClient = vertx.createHttpClient();
        proxyServer = vertx.createHttpServer().requestHandler(originalRequest -> {
            try {
                originalRequest.bodyHandler(originalRequestBody -> {
                    wasProxyCalled.set(true);
                    URI uri = URI.create(originalRequest.absoluteURI());
                    proxyClient.request(originalRequest.method(), uri.getPort(), uri.getHost(), originalRequest.uri())
                            .andThen(proxyRequest -> {
                                try {
                                    Log.info("Proxying request to " + originalRequest.absoluteURI());
                                    if (!proxyRequest.succeeded()) {
                                        proxyRequest.cause().printStackTrace();
                                        originalRequest.response().setStatusCode(500).end();
                                        return;
                                    }
                                    proxyRequest.result().send(originalRequestBody, proxyRequestResponse -> {
                                        if (proxyRequestResponse.succeeded()) {
                                            originalRequest.response()
                                                    .setStatusCode(proxyRequestResponse.result().statusCode());
                                            originalRequest.response().headers()
                                                    .setAll(proxyRequestResponse.result().headers());
                                            proxyRequestResponse.result().bodyHandler(body -> {
                                                originalRequest.response().end(body);
                                            });
                                        } else {
                                            originalRequest.response().setStatusCode(500).end();
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    originalRequest.response().setStatusCode(500).end();

                                }
                            });
                });
            } catch (Exception e) {
                e.printStackTrace();
                originalRequest.response().setStatusCode(500).end();
            }
        }).listen(3456).toCompletionStage().toCompletableFuture().get();
    }

    @AfterAll
    public static void stopProxyServer() {
        if (proxyServer != null) {
            proxyServer.close(x -> {
            });
        }
        if (proxyClient != null) {
            proxyClient.close(x -> {
            });
        }
        if (vertx != null) {
            vertx.close(x -> {
            });
        }
    }

    @Inject
    DummyApiClient client;

    @Test
    public void performQueryViaProxy() {
        assertThat(client.dummyQuery()).isEqualTo("dummy");
        assertThat(wasProxyCalled.get()).isTrue();
    }

    @GraphQLClientApi(configKey = "dummy")
    public interface DummyApiClient {
        @Query
        String dummyQuery();
    }

    @GraphQLApi
    public static class DummyApi {
        @Query
        public String dummyQuery() {
            return "dummy";
        }
    }
}
