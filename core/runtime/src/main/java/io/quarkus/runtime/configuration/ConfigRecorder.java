package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.ConfigConfig.BuildTimeMismatchAtRuntime;
import static io.quarkus.runtime.ConfigConfig.BuildTimeMismatchAtRuntime.fail;
import static io.quarkus.runtime.ConfigConfig.BuildTimeMismatchAtRuntime.warn;
import static io.smallrye.config.Expressions.withoutExpansion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@Recorder
public class ConfigRecorder {
    private static final Logger log = Logger.getLogger(ConfigRecorder.class);

    public void handleConfigChange(List<ConfigValue> buildTimeRuntimeValues) {
        Config config = Config.get();
        // Disable the BuildTime RunTime Fixed (has the highest ordinal), because a lookup will get the expected value,
        // and we have no idea if the user tried to override it in another source.
        Optional<ConfigSource> builtTimeRunTimeFixedConfigSource = config.getConfigSource("BuildTime RunTime Fixed");
        if (builtTimeRunTimeFixedConfigSource.isPresent()) {
            ConfigSource configSource = builtTimeRunTimeFixedConfigSource.get();
            if (configSource instanceof DisableableConfigSource) {
                ((DisableableConfigSource) configSource).disable();
            }
        }

        List<String> mismatches = new ArrayList<>();
        for (ConfigValue buildTimeRuntimeValue : buildTimeRuntimeValues) {
            String propertyName = buildTimeRuntimeValue.getName();
            // secret keys can only be read unlocked; the value is flagged as secret, so we do not expose it
            // the check only compares raw values, so expressions are not expanded to avoid resolving and exposing their values
            ConfigValue currentValue = SecretKeys
                    .doUnlocked(() -> withoutExpansion(() -> config.getConfigValue(propertyName)));
            // Check for changes:
            // - the config source position check tell us if the value is actually present in the config even if null
            // - compare the ordinals of the recorded to the current value, if higher or equals there is a tentative override
            // - compare the actual config values, if different there is a mismatch
            if (currentValue.getConfigSourcePosition() > 0
                    && currentValue.getSourceOrdinal() >= buildTimeRuntimeValue.getSourceOrdinal()
                    && !Objects.equals(buildTimeRuntimeValue.getRawValue(), currentValue.getRawValue())) {
                if (currentValue.isSecret()) {
                    mismatches.add(
                            " - " + propertyName + " is set to a value different from the build time fixed value."
                                    + " Did you change the property " + propertyName + " after building the application?");
                } else {
                    mismatches.add(
                            " - " + propertyName + " is set to '" + currentValue.getRawValue()
                                    + "' but it is build time fixed to '"
                                    + buildTimeRuntimeValue.getRawValue() + "'. Did you change the property " + propertyName
                                    + " after building the application?");
                }
            }
        }

        // Enable the BuildTime RunTime Fixed. It should be fine doing these operations, because this is on startup
        if (builtTimeRunTimeFixedConfigSource.isPresent()) {
            ConfigSource configSource = builtTimeRunTimeFixedConfigSource.get();
            if (configSource instanceof DisableableConfigSource) {
                ((DisableableConfigSource) configSource).enable();
            }
        }

        if (!mismatches.isEmpty()) {
            String msg = "Build time property cannot be changed at runtime:\n" + String.join("\n", mismatches);
            // TODO - This should use ConfigConfig, but for some reason, the test fails sometimes with mapping not found when looking ConfigConfig
            BuildTimeMismatchAtRuntime buildTimeMismatchAtRuntime = config
                    .getOptionalValue("quarkus.config.build-time-mismatch-at-runtime", BuildTimeMismatchAtRuntime.class)
                    .orElse(warn);
            if (fail.equals(buildTimeMismatchAtRuntime)) {
                throw new IllegalStateException(msg);
            } else if (warn.equals(buildTimeMismatchAtRuntime)) {
                log.warn(msg);
            }
        }
    }

    public void handleNativeProfileChange(List<String> buildProfiles) {
        Config config = Config.get();
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

    public void deprecatedProperties(Map<String, String> deprecatedProperties) {
        ConfigDiagnostic.deprecatedProperties(deprecatedProperties);
    }

    public void unknownConfigFiles() throws Exception {
        ConfigDiagnostic.unknownConfigFiles(ConfigDiagnostic.configFilesFromLocations());
    }

    public void releaseConfig(ShutdownContext shutdownContext) {
        // Releases the runtime configuration, registered for the runtime classloader, which is the context
        // classloader when the shutdown task runs. It does not clear QuarkusConfigFactory#config, so the build
        // time configuration, held by the QuarkusConfigFactory from the augmentation classloader, is untouched.
        // This is mostly useful to handle restarts in Dev/Test mode.
        shutdownContext.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                ((SmallRyeConfigProviderResolver) SmallRyeConfigProviderResolver.instance())
                        .releaseConfig(Thread.currentThread().getContextClassLoader());
            }
        });
    }
}
