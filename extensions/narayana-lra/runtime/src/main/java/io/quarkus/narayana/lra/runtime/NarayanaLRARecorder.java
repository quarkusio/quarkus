package io.quarkus.narayana.lra.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipant;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaLRARecorder {
    private static final Logger log = Logger.getLogger(NarayanaLRARecorder.class);

    static LRAParticipantRegistry registry;

    public void setConfig(final LRAConfiguration config) {
        if (System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY) == null) {
            System.setProperty(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, config.coordinatorURL);
        }
    }

    public void setParticipantTypes(List<String> classNames) {
        Map<String, LRAParticipant> nonJaxParticipants = new HashMap<>();

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className,
                        false, Thread.currentThread().getContextClassLoader());
                LRAParticipant lraParticipant = new LRAParticipant(clazz);

                nonJaxParticipants.put(className, lraParticipant);
            } catch (ClassNotFoundException e) {
                log.errorf("Unable to proxy class %s (%s)", className, e.getMessage());
            }
        }

        registry = new LRAParticipantRegistry(nonJaxParticipants);
    }
}
