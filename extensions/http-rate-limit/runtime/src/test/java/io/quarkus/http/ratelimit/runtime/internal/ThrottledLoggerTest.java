package io.quarkus.http.ratelimit.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.runtime.internal.gcra.MutableClock;

class ThrottledLoggerTest {

    private static final String CATEGORY = ThrottledLoggerTest.class.getName();

    final MutableClock clock = new MutableClock();
    final List<String> logged = new ArrayList<>();
    final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logged.add(String.format(record.getMessage(), record.getParameters() == null ? new Object[0]
                    : record.getParameters()));
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void listen() {
        java.util.logging.Logger.getLogger(CATEGORY).addHandler(handler);
    }

    @AfterEach
    void stopListening() {
        java.util.logging.Logger.getLogger(CATEGORY).removeHandler(handler);
    }

    ThrottledLogger logger(Duration interval) {
        return new ThrottledLoggers(clock, interval).logger(Logger.getLogger(CATEGORY));
    }

    @Test
    void firstWarningIsLoggedAtOnceAndTheNextOnesOncePerInterval() {
        ThrottledLogger log = logger(Duration.ofSeconds(10));
        log.warnf("%s %s", "first", 1);
        log.warnf("%s %s", "dropped", 2);
        clock.advance(Duration.ofSeconds(9));
        log.warnf("%s %s", "dropped", 3);
        clock.advance(Duration.ofSeconds(1));
        log.warnf("%s %s", "second", 4);
        assertThat(logged).containsExactly("first 1", "second 4");
    }

    @Test
    void eachLoggerHasItsOwnBudget() {
        ThrottledLogger one = logger(ThrottledLoggers.DEFAULT_INTERVAL);
        ThrottledLogger other = logger(ThrottledLoggers.DEFAULT_INTERVAL);
        one.warn("one", null);
        other.warn("other", null);
        one.warn("one again", null);
        assertThat(logged).containsExactly("one", "other");
    }
}
