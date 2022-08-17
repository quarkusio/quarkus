package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationRuntimeConfig.BuildTimeMismatchAtRuntime;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@Recorder
public class ConfigRecorder {

    private static final Logger log = Logger.getLogger(ConfigRecorder.class);

    final ConfigurationRuntimeConfig configurationConfig;

    public ConfigRecorder(ConfigurationRuntimeConfig configurationConfig) {
        this.configurationConfig = configurationConfig;
    }

    public void handleConfigChange(Map<String, ConfigValue> buildTimeRuntimeValues) {
        SmallRyeConfigBuilder configBuilder = ConfigUtils.emptyConfigBuilder();
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if ("BuildTime RunTime Fixed".equals(configSource.getName())) {
                continue;
            }
            configBuilder.withSources(configSource);
        }
        SmallRyeConfig config = configBuilder.build();

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, ConfigValue> entry : buildTimeRuntimeValues.entrySet()) {
            ConfigValue currentValue = config.getConfigValue(entry.getKey());
            if (currentValue.getValue() != null && !entry.getValue().getValue().equals(currentValue.getValue())
                    && entry.getValue().getSourceOrdinal() < currentValue.getSourceOrdinal()) {
                mismatches.add(
                        " - " + entry.getKey() + " is set to '" + currentValue.getValue()
                                + "' but it is build time fixed to '"
                                + entry.getValue().getValue() + "'. Did you change the property " + entry.getKey()
                                + " after building the application?");
            }
        }
        if (!mismatches.isEmpty()) {
            final String msg = "Build time property cannot be changed at runtime:\n" + String.join("\n", mismatches);
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

    public void handleNativeProfileChange(List<String> buildProfiles) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        List<String> runtimeProfiles = config.getProfiles();

        if (buildProfiles.size() != runtimeProfiles.size()) {
            log.warn(
                    "The profile '" + buildProfiles + "' used to build the native image is different from the runtime profile '"
                            + runtimeProfiles + "'. This may lead to unexpected results.");
            return;
        }

        for (int i = 0; i < buildProfiles.size(); i++) {
            String buildProfile = buildProfiles.get(i);
            String runtimeProfile = runtimeProfiles.get(i);

            if (!buildProfile.equals(runtimeProfile)) {
                log.warn("The profile '" + buildProfile
                        + "' used to build the native image is different from the runtime profile '" + runtimeProfile
                        + "'. This may lead to unexpected results.");
            }
        }
    }
}
