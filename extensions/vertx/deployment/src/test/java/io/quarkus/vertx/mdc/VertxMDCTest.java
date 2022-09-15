package io.quarkus.vertx.mdc;

import static io.quarkus.vertx.mdc.VerticleDeployer.REQUEST_ID_HEADER;
import static io.quarkus.vertx.mdc.VerticleDeployer.VERTICLE_PORT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

/*
This test was mostly based on https://github.com/reactiverse/reactiverse-contextual-logging/blob/39e691d3a8fd78d19ee120cab8d8b38a4ef67813/src/test/java/io/reactiverse/contextual/logging/ContextualLoggingIT.java
 */

public class VertxMDCTest {
    private static final Logger LOGGER = Logger.getLogger(VertxMDCTest.class);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(VerticleDeployer.class)
                            .addClass(InMemoryLogHandler.class)
                            .addClass(InMemoryLogHandlerProducer.class))
            .overrideConfigKey("quarkus.log.console.format", "%d{HH:mm:ss} %-5p requestId=%X{requestId} [%c{2.}] (%t) %s%e%n");

    @Inject
    Vertx vertx;

    @Inject
    InMemoryLogHandler inMemoryLogHandler;

    @Inject
    VerticleDeployer verticleDeployer;

    static final CountDownLatch countDownLatch = new CountDownLatch(1);
    static final AtomicReference<Throwable> errorDuringExecution = new AtomicReference<>();

    @Test
    void mdc() throws Throwable {
        List<String> requestIds = IntStream.range(0, 10)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(toList());

        sendRequests(requestIds, onSuccess(v -> {
            try {
                Map<String, List<String>> allMessagesById = inMemoryLogHandler.logRecords()
                        .stream()
                        .map(line -> line.split(" ### "))
                        .peek(split -> assertEquals(split[0], split[2]))
                        .collect(groupingBy(split -> split[0],
                                mapping(split -> split[1], toList())));

                assertEquals(requestIds.size(), allMessagesById.size());
                assertTrue(requestIds.containsAll(allMessagesById.keySet()));

                List<String> expected = Stream.<String> builder()
                        .add("Received HTTP request")
                        .add("Timer fired")
                        .add("Blocking task executed")
                        .add("Received Web Client response")
                        .build()
                        .collect(toList());

                for (List<String> messages : allMessagesById.values()) {
                    assertEquals(expected, messages);
                }
            } catch (Throwable t) {
                errorDuringExecution.set(t);
            } finally {
                countDownLatch.countDown();
            }
        }));

        countDownLatch.await();

        Throwable throwable = errorDuringExecution.get();
        if (throwable != null) {
            throw throwable;
        }
    }

    @Test
    public void mdcNonVertxThreadTest() {
        String mdcValue = "Test MDC value";
        MDC.put("requestId", mdcValue);
        LOGGER.info("Test 1");

        assertThat(inMemoryLogHandler.logRecords(),
                hasItem(mdcValue + " ### Test 1"));

        MDC.remove("requestId");
        LOGGER.info("Test 2");

        assertThat(inMemoryLogHandler.logRecords(),
                hasItem(" ### Test 2"));

        mdcValue = "New test MDC value";
        MDC.put("requestId", mdcValue);
        LOGGER.info("Test 3");

        assertThat(inMemoryLogHandler.logRecords(),
                hasItem(mdcValue + " ### Test 3"));
    }

    protected <T> Handler<AsyncResult<T>> onSuccess(Consumer<T> consumer) {
        return result -> {
            if (result.failed()) {
                errorDuringExecution.set(result.cause());
                countDownLatch.countDown();
            } else {
                consumer.accept(result.result());
            }
        };
    }

    @SuppressWarnings({ "rawtypes" })
    private void sendRequests(List<String> ids, Handler<AsyncResult<Void>> handler) {
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setDefaultPort(VERTICLE_PORT));

        HttpRequest<Buffer> request = webClient.get("/")
                .expect(ResponsePredicate.SC_OK);

        List<Future> futures = ids.stream()
                .map(id -> request.putHeader(REQUEST_ID_HEADER, id).send())
                .collect(toList());

        CompositeFuture.all(futures).<Void> mapEmpty().onComplete(handler);
    }
}
