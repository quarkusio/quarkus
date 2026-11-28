package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link LettuceTransport} writes Lettuce's transport switches: what was not set is set, what already has
 * the needed value is left alone, what has the opposite value is overridden, and everything is put back afterwards.
 */
class LettuceTransportTest {

    private static final String EPOLL = "io.lettuce.core.epoll";
    private static final String KQUEUE = "io.lettuce.core.kqueue";
    private static final String IOURING = "io.lettuce.core.iouring";

    private final Map<String, String> original = new HashMap<>();

    @BeforeEach
    void clearSwitches() {
        for (String property : List.of(EPOLL, KQUEUE, IOURING)) {
            original.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
    }

    @AfterEach
    void restoreSwitches() {
        LettuceTransport.restore();
        for (Map.Entry<String, String> entry : original.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    @Test
    void unsetSwitchesAreSetAndClearedAgain() {
        LettuceTransport.setSwitches("epoll");
        assertThat(System.getProperty(EPOLL)).isEqualTo("true");
        assertThat(System.getProperty(KQUEUE)).isEqualTo("false");
        assertThat(System.getProperty(IOURING)).isEqualTo("false");

        LettuceTransport.restore();
        assertThat(System.getProperty(EPOLL)).isNull();
        assertThat(System.getProperty(KQUEUE)).isNull();
        assertThat(System.getProperty(IOURING)).isNull();
    }

    @Test
    void switchAlreadySetToTheNeededValueIsLeftAlone() {
        // another spelling of false: the property is not rewritten
        System.setProperty(EPOLL, "FALSE");

        LettuceTransport.setSwitches("nio");
        assertThat(System.getProperty(EPOLL)).isEqualTo("FALSE");
        assertThat(System.getProperty(KQUEUE)).isEqualTo("false");

        LettuceTransport.restore();
        assertThat(System.getProperty(EPOLL)).isEqualTo("FALSE");
        assertThat(System.getProperty(KQUEUE)).isNull();
    }

    @Test
    void switchSetToTheOppositeValueIsOverriddenAndPutBack() {
        System.setProperty(EPOLL, "true");

        LettuceTransport.setSwitches("nio");
        assertThat(System.getProperty(EPOLL)).isEqualTo("false");

        LettuceTransport.restore();
        assertThat(System.getProperty(EPOLL)).isEqualTo("true");
    }

    @Test
    void restoreWithoutAlignmentChangesNothing() {
        System.setProperty(IOURING, "true");

        LettuceTransport.restore();
        assertThat(System.getProperty(IOURING)).isEqualTo("true");
    }
}
