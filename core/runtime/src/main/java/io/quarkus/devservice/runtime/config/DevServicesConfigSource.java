package io.quarkus.devservice.runtime.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesRegistry;
import io.quarkus.runtime.LaunchMode;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesConfigSource implements ConfigSource {

    RunningDevServicesRegistry tracker = new RunningDevServicesRegistry();

    private final LaunchMode launchMode;

    public DevServicesConfigSource(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public Set<String> getPropertyNames() {
        // We could make this more efficient by not invoking the supplier on the other end, but it would need a more complex interface
        Set<String> names = new HashSet<>();

        Set<Supplier<Map>> allConfig = tracker.getConfigForAllRunningServices(launchMode.name());
        if (allConfig != null) {
            for (Supplier<Map> o : allConfig) {
                Map config = o.get();
                names.addAll(config.keySet());
            }
        }
        return names;
    }

    @Override
    public String getValue(String propertyName) {
        Set<Supplier<Map>> allConfig = tracker.getConfigForAllRunningServices(launchMode.name());
        if (allConfig != null) {
            for (Supplier<Map> o : allConfig) {
                Map config = o.get();
                String answer = (String) config.get(propertyName);
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "DevServicesConfigSource";
    }

    @Override
    public int getOrdinal() {
        // See discussion on DevServicesConfigBuilder about what the right value here is
        return 10;
    }
}
