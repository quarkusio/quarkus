package io.quarkus.smallrye.reactivemessaging.amqp.devmode.nohttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.reactivemessaging.amqp.AnonymousAmqpBroker;
import io.quarkus.test.QuarkusDevModeTest;

public class AmqpDevModeNoHttpTest {
    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Producer.class, Consumer.class,
                            AnonymousAmqpBroker.class, ProtonProtocolManagerFactory.class)
                    .addAsResource("broker.xml")
                    .addAsResource("application.properties"))
            .setLogRecordPredicate(r -> r.getLoggerName().equals(Consumer.class.getName()));

    @BeforeAll
    public static void startBroker() {
        AnonymousAmqpBroker.start();
    }

    @AfterAll
    public static void stopBroker() {
        AnonymousAmqpBroker.stop();
    }

    // For all tests below: we don't know exactly when the AMQP connector receives credits from the broker
    // and then requests items from the producer, so we don't know what number will be sent to the queue first.
    // What we do know, and hence test, is the relationship between consecutive numbers in the queue.
    // See also https://github.com/smallrye/smallrye-reactive-messaging/issues/1125.

    @Test
    public void testProducerUpdate() {
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            List<LogRecord> log = TEST.getLogRecords();
            assertThat(log).hasSizeGreaterThanOrEqualTo(5);

            List<Long> nums = log.stream()
                    .map(it -> Long.parseLong(it.getMessage()))
                    .collect(Collectors.toList());

            long last = nums.get(nums.size() - 1);
            assertThat(nums).containsSequence(last - 4, last - 3, last - 2, last - 1, last);
        });

        TEST.modifySourceFile(Producer.class, s -> s.replace("* 1", "* 2"));

        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            List<LogRecord> log = TEST.getLogRecords();
            assertThat(log).hasSizeGreaterThanOrEqualTo(5);

            List<Long> nums = log.stream()
                    .map(it -> Long.parseLong(it.getMessage()))
                    .collect(Collectors.toList());

            long last = nums.get(nums.size() - 1);
            assertThat(nums).containsSequence(last - 8, last - 6, last - 4, last - 2, last);
        });
    }

    @Test
    public void testConsumerUpdate() {
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            List<LogRecord> log = new CopyOnWriteArrayList<>(TEST.getLogRecords());
            assertThat(log).hasSizeGreaterThanOrEqualTo(5);

            List<Long> nums = log.stream()
                    .map(it -> Long.parseLong(it.getMessage()))
                    .collect(Collectors.toList());

            long last = nums.get(nums.size() - 1);
            assertThat(nums).containsSequence(last - 4, last - 3, last - 2, last - 1, last);
        });

        TEST.modifySourceFile(Consumer.class, s -> s.replace("* 1", "* 3"));

        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            List<LogRecord> log = TEST.getLogRecords();
            assertThat(log).hasSizeGreaterThanOrEqualTo(5);

            List<Long> nums = log.stream()
                    .map(it -> Long.parseLong(it.getMessage()))
                    .collect(Collectors.toList());

            long last = nums.get(nums.size() - 1);
            assertThat(nums).containsSequence(last - 12, last - 9, last - 6, last - 3, last);
        });
    }
}
