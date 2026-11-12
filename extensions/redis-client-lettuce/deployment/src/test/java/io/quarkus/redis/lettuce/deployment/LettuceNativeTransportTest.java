package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
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
 * With Vert.x on the native transport (epoll on Linux, where the test class path carries it), Lettuce runs on it too;
 * elsewhere both stay on NIO. Either way the two agree, which is what the message says.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceNativeTransportTest {

    static final String EXPECTED = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")
            ? "Lettuce runs on the epoll transport Vert.x selected (EpollSocketChannel)"
            : "Lettuce runs on the nio transport Vert.x selected (NioSocketChannel)";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.vertx.native-transport", "if-available")
            .setLogRecordPredicate(record -> LettuceTransport.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.INFO.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .singleElement()
                    .satisfies(record -> assertThat(message(record))
                            .isEqualTo(EXPECTED)));

    @Inject
    RedisDataSource ds;

    @Test
    void commandsWorkOnTheNativeTransport() {
        ds.value(String.class, String.class).set("transport", "native");
        assertThat(ds.value(String.class, String.class).get("transport")).isEqualTo("native");
    }

    private static String message(LogRecord record) {
        return record instanceof ExtLogRecord ext ? ext.getFormattedMessage() : record.getMessage();
    }

}
