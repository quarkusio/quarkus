package io.quarkus.narayana.lra.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaLRAParticipantRecorder {
    public void setConfig(final LRAParticipantConfiguration config) {
        System.setProperty("lra.http.host", config.coordinatorHost);
        System.setProperty("lra.http.port", Integer.toString(config.coordinatorPort));
    }
}
