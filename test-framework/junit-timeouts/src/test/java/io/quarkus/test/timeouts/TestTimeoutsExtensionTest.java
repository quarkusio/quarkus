package io.quarkus.test.timeouts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Checks that the extension is autodetected and publishes the timeout of the running test.
 */
class TestTimeoutsExtensionTest {

    @Test
    void extensionPublishesTimeout() {
        assertThat(System.getProperty(TestTimeouts.CURRENT_PROPERTY)).isNotNull();
        assertThat(TestTimeouts.get()).isPositive();
    }
}
