package io.quarkus.devservice.runtime.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker;

// This should live in the devservices/runtime module, but that module doesn't exist, and adding it is a breaking change
public class DevServicesConfigSource implements ConfigSource {

    RunningDevServicesTracker tracker = new RunningDevServicesTracker();

    @Override
    public Set<String> getPropertyNames() {
        // We could make this more efficient by not invoking the supplier on the other end, but it would need a more complex interface
        Set<String> names = new HashSet<>();

        for (Supplier<Map> o : tracker.getConfigForAllRunningServices()) {
            Map config = o.get();
            names.addAll(config.keySet());
        }
        return names;
    }

    @Override
    public String getValue(String propertyName) {
        for (Supplier<Map> o : tracker.getConfigForAllRunningServices()) {
            Map config = o.get();
            return (String) config.get(propertyName);
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
