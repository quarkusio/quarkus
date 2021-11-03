package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationRuntimeConfig.BuildTimeMismatchAtRuntime;

@Recorder
public class ConfigRecorder {

    private static final Logger log = Logger.getLogger(ConfigRecorder.class);

    final ConfigurationRuntimeConfig configurationConfig;

    public ConfigRecorder(ConfigurationRuntimeConfig configurationConfig) {
        this.configurationConfig = configurationConfig;
    }

    public void handleConfigChange(Map<String, String> buildTimeConfig) {
        Config configProvider = ConfigProvider.getConfig();
        List<String> mismatches = null;
        for (Map.Entry<String, String> entry : buildTimeConfig.entrySet()) {
            Optional<String> val = configProvider.getOptionalValue(entry.getKey(), String.class);
            if (val.isPresent()) {
                if (!val.get().equals(entry.getValue())) {
                    if (mismatches == null) {
                        mismatches = new ArrayList<>();
                    }
                    mismatches.add(
                            " - " + entry.getKey() + " was '" + entry.getValue() + "' at build time and is now '" + val.get()
                                    + "'");
                }
            }
        }
        if (mismatches != null && !mismatches.isEmpty()) {
            final String msg = "Build time property cannot be changed at runtime:\n"
                    + mismatches.stream().collect(Collectors.joining("\n"));
            switch (configurationConfig.buildTimeMismatchAtRuntime) {
                case fail:
                    throw new IllegalStateException(msg);
                case warn:
                    log.warn(msg);
                    break;
                default:
                    throw new IllegalStateException("Unexpected " + BuildTimeMismatchAtRuntime.class.getName() + ": "
                            + configurationConfig.buildTimeMismatchAtRuntime);
            }

        }
    }
}
