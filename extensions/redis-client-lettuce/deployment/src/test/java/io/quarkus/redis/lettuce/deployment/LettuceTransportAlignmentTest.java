package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceTransport;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * With the epoll transport jar on the class path and Vert.x on NIO (the Quarkus default), Lettuce must run on NIO as
 * well, where left to itself it would pick epoll on Linux and fail to register its channels on the Vert.x event loops.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceTransportAlignmentTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setLogRecordPredicate(record -> LettuceTransport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.INFO.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .singleElement()
                    .satisfies(record -> assertThat(message(record))
                            .isEqualTo("Lettuce runs on the nio transport Vert.x selected (NioSocketChannel)")));

    @Inject
    RedisDataSource ds;

    @Test
    void commandsWorkOnTheAlignedTransport() {
        ds.value(String.class, String.class).set("transport", "nio");
        assertThat(ds.value(String.class, String.class).get("transport")).isEqualTo("nio");
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }

}
