package io.quarkus.runtime;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConfigChangeRecorder {

    private static final Logger log = Logger.getLogger(ConfigChangeRecorder.class);

    public void handleConfigChange(Map<String, String> buildTimeConfig) {
        Config configProvider = ConfigProvider.getConfig();
        for (Map.Entry<String, String> entry : buildTimeConfig.entrySet()) {
            Optional<String> val = configProvider.getOptionalValue(entry.getKey(), String.class);
            if (val.isPresent()) {
                if (!val.get().equals(entry.getValue())) {
                    log.warn("Build time property cannot be changed at runtime. " + entry.getKey() + " was "
                            + entry.getValue() + " at build time and is now " + val.get());
                }
            }
        }
    }
}
