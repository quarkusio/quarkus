package io.quarkus.vertx.mdc;

import static io.quarkus.vertx.mdc.VerticleDeployer.REQUEST_ID_HEADER;
import static io.quarkus.vertx.mdc.VerticleDeployer.VERTICLE_PORT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

/*
This test was mostly based on https://github.com/reactiverse/reactiverse-contextual-logging/blob/39e691d3a8fd78d19ee120cab8d8b38a4ef67813/src/test/java/io/reactiverse/contextual/logging/ContextualLoggingIT.java
 */
@DisabledOnOs(OS.WINDOWS)
public class VertxMDCTest {
    private static final Logger LOGGER = Logger.getLogger(VertxMDCTest.class);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(VerticleDeployer.class)
                    .addClass(InMemoryLogHandler.class).addClass(InMemoryLogHandlerProducer.class))
            .overrideConfigKey("quarkus.log.console.format",
                    "%d{HH:mm:ss} %-5p requestId=%X{requestId} [%c{2.}] (%t) %s%e%n");

    @Inject
    Vertx vertx;

    @Inject
    InMemoryLogHandler inMemoryLogHandler;
    @Inject
    InMemoryLogHandlerProducer producer;
    @Inject
    VerticleDeployer verticleDeployer;

    @RepeatedTest(10)
    void mdc() throws Throwable {

        InMemoryLogHandler.reset();
        await().until(() -> producer.isInitialized());
        await().until(InitialConfigurator.DELAYED_HANDLER::isActivated);
        await().until(() -> {
            return Arrays.stream(InitialConfigurator.DELAYED_HANDLER.getHandlers())
                    .anyMatch(h -> h == inMemoryLogHandler);
        });
        ;

        List<String> requestIds = IntStream.range(0, 1).mapToObj(i -> UUID.randomUUID().toString()).collect(toList());

        CountDownLatch done = new CountDownLatch(1);
        sendRequests(requestIds, done);

        Assertions.assertTrue(done.await(20, TimeUnit.SECONDS));

        await().untilAsserted(() -> {
            Map<String, List<String>> allMessagesById = inMemoryLogHandler.logRecords().stream()
                    .map(line -> line.split(" ### ")).peek(split -> assertEquals(split[0], split[2]))
                    .collect(groupingBy(split -> split[0], mapping(split -> split[1], toList())));

            assertEquals(requestIds.size(), allMessagesById.size());
            assertTrue(requestIds.containsAll(allMessagesById.keySet()));

            List<String> expected = Stream.<String> builder().add("Received HTTP request").add("Timer fired")
                    .add("Blocking task executed").add("Received Web Client response").build().collect(toList());

            for (List<String> messages : allMessagesById.values()) {
                assertEquals(expected, messages);
            }
        });
    }

    @RepeatedTest(10)
    public void mdcNonVertxThreadTest() {
        InMemoryLogHandler.reset();
        await().until(() -> producer.isInitialized());
        await().until(InitialConfigurator.DELAYED_HANDLER::isActivated);
        await().until(() -> {
            return Arrays.stream(InitialConfigurator.DELAYED_HANDLER.getHandlers())
                    .anyMatch(h -> h == inMemoryLogHandler);
        });
        ;

        String mdcValue = "Test MDC value";
        MDC.put("requestId", mdcValue);

        await().untilAsserted(() -> {
            LOGGER.info("Test 1");
            assertThat(inMemoryLogHandler.logRecords(), hasItem(mdcValue + " ### Test 1"));
        });

        MDC.remove("requestId");

        await().untilAsserted(() -> {
            LOGGER.info("Test 2");
            assertThat(inMemoryLogHandler.logRecords(), hasItem(" ### Test 2"));
        });

        String mdcValue2 = "New test MDC value";
        MDC.put("requestId", mdcValue2);

        await().untilAsserted(() -> {
            LOGGER.info("Test 3");
            assertThat(inMemoryLogHandler.logRecords(), hasItem(mdcValue2 + " ### Test 3"));
        });
    }

    private void sendRequests(List<String> ids, CountDownLatch done) {
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setDefaultPort(VERTICLE_PORT));

        HttpRequest<Buffer> request = webClient.get("/").expect(ResponsePredicate.SC_OK);

        List<? extends Future<?>> futures = ids.stream().map(id -> request.putHeader(REQUEST_ID_HEADER, id).send())
                .collect(toList());

        Future.all(futures).mapEmpty().onComplete(x -> {
            done.countDown();
        });
    }
}
