package io.quarkus.runtime.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationRuntimeConfig.BuildTimeMismatchAtRuntime;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ExpressionConfigSourceInterceptor;
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
        // Create a new Config without the "BuildTime RunTime Fixed" sources to check for different values
        SmallRyeConfigBuilder configBuilder = ConfigUtils.emptyConfigBuilder();
        // We need to disable the expression resolution, because we may be missing expressions from the "BuildTime RunTime Fixed" source
        configBuilder.withDefaultValue(Config.PROPERTY_EXPRESSIONS_ENABLED, "false");
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if ("BuildTime RunTime Fixed".equals(configSource.getName())) {
                continue;
            }
            configBuilder.withSources(configSource);
        }
        // Add a new expression resolution to fall back to the current Config if we cannot expand the expression
        configBuilder.withInterceptors(new ExpressionConfigSourceInterceptor() {
            @Override
            public io.smallrye.config.ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                return super.getValue(new ConfigSourceInterceptorContext() {
                    @Override
                    public io.smallrye.config.ConfigValue proceed(final String name) {
                        io.smallrye.config.ConfigValue configValue = context.proceed(name);
                        if (configValue == null) {
                            configValue = (io.smallrye.config.ConfigValue) ConfigProvider.getConfig().getConfigValue(name);
                            if (configValue.getValue() == null) {
                                return null;
                            }
                        }
                        return configValue;
                    }

                    @Override
                    public Iterator<String> iterateNames() {
                        return context.iterateNames();
                    }

                    @Override
                    public Iterator<io.smallrye.config.ConfigValue> iterateValues() {
                        return context.iterateValues();
                    }
                }, name);
            }
        });
        SmallRyeConfig config = configBuilder.build();

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, ConfigValue> entry : buildTimeRuntimeValues.entrySet()) {
            ConfigValue currentValue = config.getConfigValue(entry.getKey());
            // Check for changes. Also, we only have a change if the source ordinal is higher
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
