package io.quarkus.devservice.runtime.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.devservices.crossclassloader.runtime.RunningService;
import io.quarkus.runtime.LaunchMode;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesOverrideConfigSource implements ConfigSource {

    private final LaunchMode launchMode;

    public DevServicesOverrideConfigSource(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public Set<String> getPropertyNames() {
        // We could make this more efficient by not invoking the supplier on the other end, but it would need a more complex interface
        Set<String> names = new HashSet<>();

        Set<RunningService> allConfig = RunningDevServicesRegistry.INSTANCE.getAllRunningServices(launchMode.name());
        if (allConfig != null) {
            for (RunningService service : allConfig) {
                Map<String, String> config = service.overrideConfigs();
                names.addAll(config.keySet());
            }
        }
        return names;
    }

    @Override
    public String getValue(String propertyName) {
        Set<RunningService> allConfig = RunningDevServicesRegistry.INSTANCE.getAllRunningServices(launchMode.name());
        if (allConfig != null) {
            for (RunningService service : allConfig) {
                Map<String, String> config = service.overrideConfigs();
                String answer = config.get(propertyName);
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "DevServicesOverrideConfigSource";
    }

    @Override
    public int getOrdinal() {
        // This is a dynamic override, so it needs a high priority to be able to override ephemeral port (0) with real values
        return 410; // a bit more than system properties
    }
}
