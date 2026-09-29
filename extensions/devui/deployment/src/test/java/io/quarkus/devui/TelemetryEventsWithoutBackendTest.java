package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

/**
 * Without a metrics or tracing backend nothing receives telemetry events, so they stay switched off and senders do
 * not build them for nobody. {@link TelemetryEvents} is loaded parent first, so the test sees the application's flag.
 */
public class TelemetryEventsWithoutBackendTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public TelemetryEventsWithoutBackendTest() {
        super("devui");
    }

    @Test
    public void eventsStayOffWithoutABackend() throws Exception {
        // The application is up: its build time data is served.
        assertThat(super.getAllKeys()).isNotEmpty();

        assertThat(TelemetryEvents.isEnabled()).isFalse();
    }
}
