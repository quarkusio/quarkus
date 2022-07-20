package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxServerOpenTelemetryPolicyIgnoreTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(SimpleBean.class, TestSpanExporter.class)
                    .add(new StringAsset("quarkus.http.tracing-policy=IGNORE"), "application.properties"));

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URI uri;

    @Inject
    EventBus eventBus;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void testPolicyIgnore() throws Exception {
        Future<HttpResponse<Buffer>> response = WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/hello")
                .send();

        response.onComplete(event -> {
            assertTrue(event.succeeded());
            spanExporter.getFinishedSpanItems(0);
        });
    }

    @Test
    void testPolicyIgnoreEventBus() throws Exception {
        final DeliveryOptions options = new DeliveryOptions();
        options.setTracingPolicy(TracingPolicy.IGNORE);

        SimpleBean.MESSAGES.clear();
        SimpleBean.latch = new CountDownLatch(2);
        eventBus.publish("pub", "Hello", options);
        SimpleBean.latch.await(2, TimeUnit.SECONDS);
        assertTrue(SimpleBean.MESSAGES.isEmpty());
    }

    @ApplicationScoped
    public static class HelloRouter {
        @Inject
        Router router;
        @Inject
        Vertx vertx;

        public void register(@Observes StartupEvent ev) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }

    static class SimpleBean {

        static volatile CountDownLatch latch;

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        @ConsumeEvent("pub")
        void consume(String message) {
            MESSAGES.add(message.toLowerCase());
            latch.countDown();
        }

        @ConsumeEvent("pub")
        void consume(Message<String> message) {
            MESSAGES.add(message.body().toUpperCase());
            latch.countDown();
        }
    }
}
