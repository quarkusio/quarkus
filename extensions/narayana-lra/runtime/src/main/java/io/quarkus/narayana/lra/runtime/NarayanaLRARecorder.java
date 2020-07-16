package io.quarkus.narayana.lra.runtime;

import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaLRARecorder {
    public void setConfig(final LRAConfiguration config) {
        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(config.nodeName);
            // set the directory for the transaction logs (NB this sets the value on all store types
            // (ie the action, state and communication stores)
            arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(config.storeDirectory);
            System.setProperty("lra.http.host", config.coordinatorHost);
            System.setProperty("lra.http.port", Integer.toString(config.coordinatorPort));
        } catch (CoreEnvironmentBeanException e) {
            throw new RuntimeException(e);
        }
    }
}
