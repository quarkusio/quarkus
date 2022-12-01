package io.quarkus.narayana.stm.runtime;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaSTMRecorder {
    public void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }
}
