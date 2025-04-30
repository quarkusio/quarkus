package io.quarkus.runtime.configuration;

import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;

@Recorder
public class DeprecatedRuntimePropertiesRecorder {

    private static final Logger log = Logger.getLogger(DeprecatedRuntimePropertiesRecorder.class);

    public void reportDeprecatedProperties(Set<String> deprecatedRuntimeProperties) {
        Config config = ConfigProvider.getConfig();
        for (String property : config.getPropertyNames()) {
            if (deprecatedRuntimeProperties.contains(property)) {
                String configSourceName = ((SmallRyeConfig) config).getConfigValue(property).getConfigSourceName();
                // this condition can be removed when support of the @ConfigRoot annotation on classes is removed
                if (DefaultValuesConfigSource.NAME.equals(configSourceName)) {
                    continue;
                }
                log.warnf("The '%s' config property is deprecated and should not be used anymore", property);
            }
        }
    }
}
