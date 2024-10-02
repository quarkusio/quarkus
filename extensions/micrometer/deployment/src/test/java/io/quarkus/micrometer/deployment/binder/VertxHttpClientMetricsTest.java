package io.quarkus.micrometer.deployment.binder;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.test.ClientDummyTag;
import io.quarkus.micrometer.test.ClientHeaderTag;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.core.http.WebSocket;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.ext.web.handler.BodyHandler;

public class VertxHttpClientMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(App.class, HttpClient.class, WsClient.class, Util.class,
                            ClientDummyTag.class, ClientHeaderTag.class));

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Inject
    HttpClient client;

    @Inject
    App server;

    @Inject
    WsClient ws;

    private Search getMeter(String name) {
        return registry.find(name);
    }

    @Test
    void testWebClientMetrics() {
        server.start();
        client.init();

        // If the WS test runs before, some data was already written
        double sizeBefore = 0;
        if (getMeter("http.client.bytes.written").summary() != null) {
            sizeBefore = registry.find("http.client.bytes.written")
                    .tag("clientName", "my-client")
                    .summary().totalAmount();
        }

        try {
            Assertions.assertEquals("ok", client.get(null));
            Assertions.assertEquals("ok", client.get("bar"));
            Assertions.assertEquals("HELLO", client.post("hello"));

            Assertions.assertNotNull(getMeter("http.client.connections").longTaskTimer());

            // Body sizes
            double expectedBytesWritten = sizeBefore + 5;
            await().untilAsserted(
                    () -> Assertions.assertEquals(expectedBytesWritten,
                            registry.find("http.client.bytes.written")
                                    .tag("clientName", "my-client").summary().totalAmount()));
            await().untilAsserted(() -> Assertions.assertEquals(9,
                    registry.find("http.client.bytes.read")
                            .tag("clientName", "my-client").summary().totalAmount()));

            await().until(() -> getMeter("http.client.requests").timer().totalTime(TimeUnit.NANOSECONDS) > 0);
            await().until(() -> {
                // Because of the different tag, the timer got called a single time
                return getMeter("http.client.requests").timer().count() == 1;
            });

            Assertions.assertEquals(1, registry.find("http.client.requests")
                    .tag("uri", "root")
                    .tag("dummy", "value")
                    .tag("foo", "UNSET")
                    .tag("outcome", "SUCCESS").timers().size(),
                    Util.foundClientRequests(registry, "/ with tag outcome=SUCCESS."));

            Assertions.assertEquals(1, registry.find("http.client.requests")
                    .tag("uri", "root")
                    .tag("dummy", "value")
                    .tag("foo", "bar")
                    .tag("outcome", "SUCCESS").timers().size(),
                    Util.foundClientRequests(registry, "/ with tag outcome=SUCCESS."));

            // Queue
            Assertions.assertEquals(3, registry.find("http.client.queue.delay")
                    .tag("clientName", "my-client").timer().count());
            Assertions.assertTrue(registry.find("http.client.queue.delay")
                    .tag("clientName", "my-client").timer().totalTime(TimeUnit.NANOSECONDS) > 0);

            await().until(() -> getMeter("http.client.queue.size").gauge().value() == 0.0);
        } finally {
            server.stop();
        }
    }

    @Test
    void testWebSocket() {
        server.start();
        try {
            ws.send("hello");
            ws.send("how are you?");
            Assertions.assertNotNull(getMeter("http.client.websocket.connections").gauge());
        } finally {
            server.stop();
        }
    }

    @ApplicationScoped
    static class App {

        @Inject
        Vertx vertx;
        private HttpServer server;

        public void start() {
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            router.get().handler(rc -> rc.endAndForget("ok"));
            router.post("/post")
                    .handler(rc -> rc.response().endAndForget(rc.body().asString().toUpperCase()));
            server = vertx.createHttpServer()
                    .requestHandler(req -> {
                        if (!req.path().endsWith("/ws")) {
                            router.handle(req);
                        } else {
                            req.toWebSocket()
                                    .subscribe().with(socket -> {
                                        socket.handler(buffer -> {
                                            socket.writeAndForget(Buffer.buffer(buffer.toString().toUpperCase()));
                                        });
                                    });
                        }
                    })
                    .listenAndAwait(8888);
        }

        public void stop() {
            server.closeAndAwait();
        }

    }

    @ApplicationScoped
    static class WsClient {
        @Inject
        Vertx vertx;
        private WebSocket client;

        @PostConstruct
        public void init() {
            client = vertx.createHttpClient(new HttpClientOptions().setShared(false)
                    .setMetricsName("ws")).webSocket(8888, "localhost", "/ws")
                    .await().indefinitely();
            client.handler(b -> {
                // Do nothing
            });
        }

        public void send(String s) {
            client.writeAndAwait(Buffer.buffer(s));
        }

        @PreDestroy
        public void cleanup() {
            client.close();
        }
    }

    @ApplicationScoped
    static class HttpClient {
        @Inject
        Vertx vertx;
        private WebClient client;

        public void init() {
            client = WebClient.create(vertx, new WebClientOptions()
                    .setMetricsName("http-client|my-client"));
        }

        public String get(String fooHeaderValue) {
            HttpRequest<Buffer> request = client.getAbs("http://localhost:8888/");
            if (fooHeaderValue != null) {
                request.putHeader("Foo", fooHeaderValue);
            }
            return request
                    .send()
                    .map(HttpResponse::bodyAsString)
                    .await().atMost(Duration.ofSeconds(10));
        }

        public String post(String payload) {
            return client.postAbs("http://localhost:8888/post")
                    .sendBuffer(Buffer.buffer(payload))
                    .map(HttpResponse::bodyAsString)
                    .await().atMost(Duration.ofSeconds(10));
        }

        @PreDestroy
        public void cleanup() {
            client.close();
        }
    }

}
