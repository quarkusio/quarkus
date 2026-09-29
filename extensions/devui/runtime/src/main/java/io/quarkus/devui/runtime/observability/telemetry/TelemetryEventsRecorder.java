package io.quarkus.devui.runtime.observability.telemetry;

import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TelemetryEventsRecorder {

    /**
     * Switches telemetry events on before any application code runs. A startup observer would be too late: spans
     * ended by earlier startup observers, migrations or {@code @Startup} beans would be dropped by senders that saw
     * events still switched off.
     */
    public void enable() {
        TelemetryEvents.setEnabled(true);
    }
}
