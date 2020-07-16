package io.quarkus.narayana.lra.runtime;

import io.narayana.lra.client.NarayanaLRAClient;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaLRARecorder {
    public void setConfig(final LRAConfiguration config) {
        if (System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY) == null) {
            System.setProperty(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, config.coordinatorURL);
        }
    }
}
