package io.quarkus.narayana.lra.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.client.internal.NarayanaLRAClient;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipant;
import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaLRARecorder {
    private static final Logger log = Logger.getLogger(NarayanaLRARecorder.class);

    static LRAParticipantRegistry registry;

    private final RuntimeValue<LRAConfiguration> runtimeConfig;

    public NarayanaLRARecorder(final RuntimeValue<LRAConfiguration> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void setConfig() {
        LRAConfiguration lraConfig = runtimeConfig.getValue();
        // once this is unified in LRA project, we can move this to RelocateConfigSourceInterceptor
        setValue(NarayanaLRAClient.LRA_COORDINATOR_URL_KEY, lraConfig.coordinatorURL());
        if (lraConfig.baseUri().isPresent()) {
            setValue(LRAConstants.NARAYANA_LRA_BASE_URI_PROPERTY_NAME, lraConfig.baseUri().get());
        }
    }

    private static void setValue(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
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
